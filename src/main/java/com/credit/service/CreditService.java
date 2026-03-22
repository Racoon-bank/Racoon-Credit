package com.credit.service;

import com.credit.client.CoreServiceClient;
import com.credit.dto.*;
import com.credit.entity.*;
import com.credit.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditService {

    private final CreditRepository creditRepository;
    private final CreditApplicationRepository creditApplicationRepository;
    private final CreditTariffRepository tariffRepository;
    private final CreditPaymentRepository paymentRepository;
    private final PaymentScheduleRepository scheduleRepository;
    private final CoreServiceClient coreServiceClient;
    private final MasterAccountService masterAccountService;

    @Transactional
    public TakeCreditResultResponse takeCredit(String userId, String authHeader, TakeCreditRequest request) {
        log.info("Taking new credit for owner: {}", userId);
        CreditTariff tariff = tariffRepository.findById(request.getTariffId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tariff not found with id: " + request.getTariffId()));
        validateBankAccountOwnership(authHeader, request.getBankAccountId());

        CreditRatingResponse rating = getUserCreditRating(userId);
        if (rating.getScore() < 500) {
            CreditApplication application = new CreditApplication();
            application.setOwnerId(userId);
            application.setBankAccountId(request.getBankAccountId());
            application.setTariff(tariff);
            application.setAmount(request.getAmount());
            application.setDurationMonths(request.getDurationMonths());
            application.setCreditRating(rating.getScore());
            application.setStatus(CreditApplicationStatus.PENDING);
            CreditApplication savedApplication = creditApplicationRepository.save(application);
            return new TakeCreditResultResponse(
                    "APPLICATION_CREATED",
                    "Кредит отправлен на рассмотрение сотруднику",
                    null,
                    mapApplicationToResponse(savedApplication)
            );
        }

        Credit credit = issueCredit(userId, request.getBankAccountId(), tariff, request.getAmount(), request.getDurationMonths());
        return new TakeCreditResultResponse("CREDIT_ISSUED", "Кредит успешно оформлен", mapToResponse(credit), null);
    }

    @Transactional
    public CreditPaymentResponse repayCredit(String userId, String authHeader, Long creditId, RepayCreditRequest request) {
        log.info("Repaying credit {} with amount {} for user {}", creditId, request.getAmount(), userId);
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit not found with id: " + creditId));
        if (!credit.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: this credit does not belong to you");
        }
        if (credit.getStatus() != CreditStatus.ACTIVE && credit.getStatus() != CreditStatus.OVERDUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit cannot be repaid. Current status: " + credit.getStatus());
        }
        validateBankAccountOwnership(authHeader, request.getBankAccountId());
        coreServiceClient.payCredit(request.getBankAccountId(), new MoneyOperationDto(request.getAmount()));

        PaymentProcessingResult result = applyPaymentToSchedules(credit, request.getAmount(), PaymentType.MANUAL_REPAYMENT, LocalDateTime.now());
        refreshCreditState(credit);
        creditRepository.save(credit);
        return mapPaymentToResponse(result.payment());
    }

    @Transactional
    public boolean tryAutomaticRepayment(Credit credit, PaymentSchedule schedule) {
        if (credit.getBankAccountId() == null || credit.getBankAccountId().isBlank()) {
            return false;
        }
        BigDecimal dueAmount = getRemainingPenalty(schedule).add(getRemainingInterest(schedule)).add(getRemainingPrincipal(schedule)).setScale(2, RoundingMode.HALF_UP);
        if (dueAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        try {
            coreServiceClient.payCredit(credit.getBankAccountId(), new MoneyOperationDto(dueAmount));
        } catch (Exception e) {
            log.warn("Automatic repayment failed for credit {}: {}", credit.getId(), e.getMessage());
            return false;
        }
        applyPaymentToSchedules(credit, dueAmount, PaymentType.AUTOMATIC_DAILY, LocalDateTime.now());
        refreshCreditState(credit);
        creditRepository.save(credit);
        return true;
    }

    @Transactional(readOnly = true)
    public CreditResponse getCreditById(Long id) {
        Credit credit = creditRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit not found with id: " + id));
        return mapToResponse(credit);
    }

    @Transactional(readOnly = true)
    public List<CreditResponse> getAllCredits() {
        return creditRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CreditResponse> getCreditsByUserId(String userId) {
        return creditRepository.findByOwnerId(userId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CreditApplicationResponse> getMyCreditApplications(String userId) {
        return creditApplicationRepository.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapApplicationToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CreditApplicationResponse> getPendingCreditApplications() {
        return creditApplicationRepository.findByStatusOrderByCreatedAtAsc(CreditApplicationStatus.PENDING).stream()
                .map(this::mapApplicationToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TakeCreditResultResponse approveCreditApplication(Long applicationId, String employeeId, CreditApplicationDecisionRequest request) {
        CreditApplication application = getPendingApplication(applicationId);
        Credit credit = issueCredit(application.getOwnerId(), application.getBankAccountId(), application.getTariff(), application.getAmount(), application.getDurationMonths());
        application.setStatus(CreditApplicationStatus.APPROVED);
        application.setReviewedBy(employeeId);
        application.setReviewedAt(LocalDateTime.now());
        application.setEmployeeComment(request != null ? request.getComment() : null);
        CreditApplication savedApplication = creditApplicationRepository.save(application);
        return new TakeCreditResultResponse("CREDIT_ISSUED", "Заявка одобрена, кредит оформлен", mapToResponse(credit), mapApplicationToResponse(savedApplication));
    }

    @Transactional
    public CreditApplicationResponse rejectCreditApplication(Long applicationId, String employeeId, CreditApplicationDecisionRequest request) {
        CreditApplication application = getPendingApplication(applicationId);
        application.setStatus(CreditApplicationStatus.REJECTED);
        application.setReviewedBy(employeeId);
        application.setReviewedAt(LocalDateTime.now());
        application.setEmployeeComment(request != null ? request.getComment() : null);
        return mapApplicationToResponse(creditApplicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public List<CreditPaymentResponse> getCreditPayments(Long creditId) {
        return paymentRepository.findByCreditIdOrderByPaymentDateDesc(creditId).stream()
                .map(this::mapPaymentToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CreditStatisticsResponse getCreditStatistics(Long creditId) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Credit not found with id: " + creditId));
        List<PaymentSchedule> schedule = scheduleRepository.findByCreditIdOrderByMonthNumber(creditId);
        BigDecimal totalInterest = schedule.stream().map(PaymentSchedule::getInterestPayment).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalToRepay = credit.getAmount().add(totalInterest);
        return new CreditStatisticsResponse(
                credit.getId(),
                credit.getAmount(),
                credit.getMonthlyPayment(),
                credit.getDurationMonths(),
                totalToRepay,
                totalInterest,
                credit.getTariff().getInterestRate().multiply(BigDecimal.valueOf(100)).stripTrailingZeros()
        );
    }

    @Transactional(readOnly = true)
    public CreditRatingResponse getUserCreditRating(String userId) {
        List<Credit> credits = creditRepository.findByOwnerId(userId);
        List<PaymentSchedule> schedules = credits.stream()
                .flatMap(credit -> scheduleRepository.findByCreditIdOrderByMonthNumber(credit.getId()).stream())
                .collect(Collectors.toList());

        int totalCredits = credits.size();
        int activeCredits = (int) credits.stream().filter(credit -> credit.getStatus() == CreditStatus.ACTIVE || credit.getStatus() == CreditStatus.OVERDUE).count();
        int currentOverduePayments = (int) schedules.stream().filter(schedule -> schedule.getPaymentStatus() == PaymentScheduleStatus.OVERDUE).count();
        int historicalOverduePayments = (int) schedules.stream().filter(this::hasHistoricalOverdue).count();
        int maxCurrentOverdueDays = schedules.stream()
                .filter(schedule -> schedule.getPaymentStatus() == PaymentScheduleStatus.OVERDUE)
                .map(PaymentSchedule::getOverdueDays)
                .max(Integer::compareTo)
                .orElse(0);
        int completedCreditsWithoutOverdues = (int) credits.stream()
                .filter(credit -> credit.getStatus() == CreditStatus.PAID_OFF)
                .filter(credit -> scheduleRepository.findByCreditIdOrderByMonthNumber(credit.getId()).stream().noneMatch(this::hasHistoricalOverdue))
                .count();

        List<PaymentSchedule> matureSchedules = schedules.stream()
                .filter(schedule -> schedule.getPaymentDate().isBefore(LocalDateTime.now()) || schedule.getPaymentStatus() == PaymentScheduleStatus.PAID)
                .collect(Collectors.toList());
        long onTimeSchedules = matureSchedules.stream()
                .filter(schedule -> schedule.getPaymentStatus() == PaymentScheduleStatus.PAID)
                .filter(schedule -> !hasHistoricalOverdue(schedule))
                .count();
        BigDecimal onTimePaymentRatio = matureSchedules.isEmpty()
                ? BigDecimal.ONE
                : BigDecimal.valueOf(onTimeSchedules).divide(BigDecimal.valueOf(matureSchedules.size()), 4, RoundingMode.HALF_UP);
        BigDecimal totalRemainingDebt = credits.stream().map(Credit::getRemainingAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        int score = 650;
        score -= currentOverduePayments * 45;
        score -= Math.min(60, maxCurrentOverdueDays * 2);
        score -= historicalOverduePayments * 15;
        score += completedCreditsWithoutOverdues * 25;
        score += calculateOnTimeRatioBonus(onTimePaymentRatio);
        score -= calculateDebtLoadPenalty(activeCredits, totalRemainingDebt);
        score = Math.max(300, Math.min(850, score));

        return new CreditRatingResponse(
                userId, score, determineRatingLevel(score), totalCredits, activeCredits,
                completedCreditsWithoutOverdues, currentOverduePayments, historicalOverduePayments,
                maxCurrentOverdueDays, onTimePaymentRatio, totalRemainingDebt, LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentScheduleResponse> getPaymentSchedule(Long creditId) {
        return scheduleRepository.findByCreditIdOrderByMonthNumber(creditId).stream()
                .map(this::mapScheduleToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OverduePaymentResponse> getOverduePayments(Long creditId) {
        return scheduleRepository.findByCreditIdAndPaymentStatusOrderByPaymentDateAsc(creditId, PaymentScheduleStatus.OVERDUE).stream()
                .map(this::mapOverdueScheduleToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OverduePaymentResponse> getMyOverduePayments(String userId) {
        return scheduleRepository.findByCreditOwnerIdAndPaymentStatusOrderByPaymentDateAsc(userId, PaymentScheduleStatus.OVERDUE).stream()
                .map(this::mapOverdueScheduleToResponse)
                .collect(Collectors.toList());
    }

    private void validateBankAccountOwnership(String authHeader, String bankAccountId) {
        List<BankAccountDto> accounts;
        try {
            accounts = coreServiceClient.getMyBankAccounts(authHeader);
        } catch (Exception e) {
            throw new RuntimeException("Unable to verify bank account ownership: " + e.getMessage(), e);
        }
        boolean owned = accounts.stream().anyMatch(a -> bankAccountId.equalsIgnoreCase(a.getId()));
        if (!owned) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: bank account does not belong to the authenticated user");
        }
    }

    private CreditResponse mapToResponse(Credit credit) {
        return new CreditResponse(
                credit.getId(),
                credit.getOwnerId(),
                credit.getTariff().getId(),
                credit.getTariff().getName(),
                credit.getTariff().getInterestRate().multiply(BigDecimal.valueOf(100)).stripTrailingZeros(),
                credit.getAmount(),
                credit.getRemainingAmount(),
                credit.getMonthlyPayment(),
                credit.getMonthlyPayment().multiply(BigDecimal.valueOf(credit.getDurationMonths())).setScale(2, RoundingMode.HALF_UP),
                credit.getDurationMonths(),
                credit.getRemainingMonths(),
                credit.getAccumulatedPenalty(),
                credit.getOverdueDays(),
                credit.getStatus(),
                credit.getIssueDate(),
                credit.getNextPaymentDate(),
                credit.getCreatedAt(),
                credit.getUpdatedAt()
        );
    }

    private CreditApplicationResponse mapApplicationToResponse(CreditApplication application) {
        return new CreditApplicationResponse(
                application.getId(),
                application.getOwnerId(),
                application.getBankAccountId(),
                application.getTariff().getId(),
                application.getTariff().getName(),
                application.getAmount(),
                application.getDurationMonths(),
                application.getCreditRating(),
                application.getStatus(),
                application.getEmployeeComment(),
                application.getReviewedBy(),
                application.getReviewedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    private CreditPaymentResponse mapPaymentToResponse(CreditPayment payment) {
        return new CreditPaymentResponse(
                payment.getId(),
                payment.getCredit().getId(),
                payment.getAmount(),
                payment.getPaymentType(),
                payment.getPaymentDate(),
                payment.getCreatedAt()
        );
    }

    private void generatePaymentSchedule(Credit credit, BigDecimal monthlyRate, BigDecimal monthlyPayment) {
        BigDecimal remainingBalance = credit.getAmount();
        LocalDateTime paymentDate = credit.getIssueDate().plusMinutes(1);
        for (int month = 1; month <= credit.getDurationMonths(); month++) {
            BigDecimal interestPayment = remainingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPayment = monthlyPayment.subtract(interestPayment);
            if (month == credit.getDurationMonths()) {
                principalPayment = remainingBalance;
            }
            remainingBalance = remainingBalance.subtract(principalPayment);
            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }
            PaymentSchedule schedule = new PaymentSchedule();
            schedule.setCredit(credit);
            schedule.setMonthNumber(month);
            schedule.setPaymentDate(paymentDate);
            schedule.setTotalPayment(interestPayment.add(principalPayment));
            schedule.setInterestPayment(interestPayment);
            schedule.setPrincipalPayment(principalPayment);
            schedule.setRemainingBalance(remainingBalance);
            schedule.setPaid(false);
            schedule.setPaymentStatus(PaymentScheduleStatus.PLANNED);
            schedule.setPenaltyAmount(BigDecimal.ZERO);
            schedule.setPaidPenaltyAmount(BigDecimal.ZERO);
            schedule.setPaidInterestAmount(BigDecimal.ZERO);
            schedule.setPaidPrincipalAmount(BigDecimal.ZERO);
            schedule.setOverdueDays(0);
            scheduleRepository.save(schedule);
            paymentDate = paymentDate.plusMinutes(1);
        }
    }

    private PaymentScheduleResponse mapScheduleToResponse(PaymentSchedule schedule) {
        return new PaymentScheduleResponse(
                schedule.getId(),
                schedule.getCredit().getId(),
                schedule.getMonthNumber(),
                schedule.getPaymentDate(),
                schedule.getTotalPayment(),
                schedule.getInterestPayment(),
                schedule.getPrincipalPayment(),
                schedule.getRemainingBalance(),
                schedule.getPaid(),
                schedule.getPaymentStatus(),
                schedule.getPenaltyAmount(),
                schedule.getPaidPenaltyAmount(),
                schedule.getPaidInterestAmount(),
                schedule.getPaidPrincipalAmount(),
                schedule.getOverdueDays(),
                schedule.getPaidAt()
        );
    }

    private OverduePaymentResponse mapOverdueScheduleToResponse(PaymentSchedule schedule) {
        BigDecimal paidAmount = schedule.getPaidPenaltyAmount().add(schedule.getPaidInterestAmount()).add(schedule.getPaidPrincipalAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingDue = schedule.getTotalPayment().add(schedule.getPenaltyAmount()).subtract(paidAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new OverduePaymentResponse(
                schedule.getId(),
                schedule.getCredit().getId(),
                schedule.getMonthNumber(),
                schedule.getPaymentDate(),
                schedule.getTotalPayment(),
                paidAmount,
                remainingDue,
                schedule.getInterestPayment(),
                schedule.getPrincipalPayment(),
                schedule.getPenaltyAmount(),
                schedule.getPaidPenaltyAmount(),
                schedule.getPaidInterestAmount(),
                schedule.getPaidPrincipalAmount(),
                schedule.getOverdueDays(),
                schedule.getPaymentStatus()
        );
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal amount, BigDecimal monthlyRate, int durationMonths) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount.divide(BigDecimal.valueOf(durationMonths), 2, RoundingMode.HALF_UP);
        }
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRatePowN = onePlusRate.pow(durationMonths);
        return amount.multiply(monthlyRate.multiply(onePlusRatePowN))
                .divide(onePlusRatePowN.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal applyAmount(BigDecimal availableAmount, BigDecimal dueAmount, BigDecimal alreadyPaid, Consumer<BigDecimal> paidAmountSetter) {
        if (availableAmount.compareTo(BigDecimal.ZERO) <= 0 || dueAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal applied = availableAmount.min(dueAmount).setScale(2, RoundingMode.HALF_UP);
        paidAmountSetter.accept(alreadyPaid.add(applied).setScale(2, RoundingMode.HALF_UP));
        return applied;
    }

    private void updateScheduleStatus(PaymentSchedule schedule, LocalDateTime now) {
        BigDecimal remainingDue = schedule.getPenaltyAmount()
                .add(schedule.getInterestPayment())
                .add(schedule.getPrincipalPayment())
                .subtract(schedule.getPaidPenaltyAmount())
                .subtract(schedule.getPaidInterestAmount())
                .subtract(schedule.getPaidPrincipalAmount());
        if (remainingDue.compareTo(BigDecimal.ZERO) <= 0) {
            schedule.setPaid(true);
            schedule.setPaymentStatus(PaymentScheduleStatus.PAID);
            schedule.setPaidAt(now);
            schedule.setOverdueDays(0);
            return;
        }
        schedule.setPaid(false);
        if (schedule.getPaymentDate().isBefore(now)) {
            schedule.setPaymentStatus(PaymentScheduleStatus.OVERDUE);
        } else if (schedule.getPaidPenaltyAmount().add(schedule.getPaidInterestAmount()).add(schedule.getPaidPrincipalAmount()).compareTo(BigDecimal.ZERO) > 0) {
            schedule.setPaymentStatus(PaymentScheduleStatus.PARTIALLY_PAID);
            schedule.setOverdueDays(0);
        } else {
            schedule.setPaymentStatus(PaymentScheduleStatus.PLANNED);
            schedule.setOverdueDays(0);
        }
    }

    private void refreshCreditState(Credit credit) {
        List<PaymentSchedule> schedules = scheduleRepository.findByCreditIdOrderByMonthNumber(credit.getId());
        BigDecimal remainingAmount = schedules.stream().map(this::getRemainingPrincipal).reduce(BigDecimal.ZERO, BigDecimal::add).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal accumulatedPenalty = schedules.stream().map(this::getRemainingPenalty).reduce(BigDecimal.ZERO, BigDecimal::add).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        int remainingMonths = (int) schedules.stream().filter(schedule -> schedule.getPaymentStatus() != PaymentScheduleStatus.PAID).count();
        int overdueDays = schedules.stream()
                .filter(schedule -> schedule.getPaymentStatus() == PaymentScheduleStatus.OVERDUE)
                .map(PaymentSchedule::getOverdueDays)
                .max(Integer::compareTo)
                .orElse(0);
        credit.setRemainingAmount(remainingAmount);
        credit.setAccumulatedPenalty(accumulatedPenalty);
        credit.setRemainingMonths(remainingMonths);
        credit.setOverdueDays(overdueDays);
        credit.setNextPaymentDate(schedules.stream()
                .filter(schedule -> schedule.getPaymentStatus() != PaymentScheduleStatus.PAID)
                .map(PaymentSchedule::getPaymentDate)
                .min(LocalDateTime::compareTo)
                .orElse(null));
        if (remainingMonths == 0 && remainingAmount.compareTo(BigDecimal.ZERO) <= 0 && accumulatedPenalty.compareTo(BigDecimal.ZERO) <= 0) {
            credit.setStatus(CreditStatus.PAID_OFF);
        } else if (overdueDays > 0) {
            credit.setStatus(CreditStatus.OVERDUE);
        } else {
            credit.setStatus(CreditStatus.ACTIVE);
        }
    }

    private BigDecimal getRemainingPenalty(PaymentSchedule schedule) {
        return schedule.getPenaltyAmount().subtract(schedule.getPaidPenaltyAmount()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getRemainingInterest(PaymentSchedule schedule) {
        return schedule.getInterestPayment().subtract(schedule.getPaidInterestAmount()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getRemainingPrincipal(PaymentSchedule schedule) {
        return schedule.getPrincipalPayment().subtract(schedule.getPaidPrincipalAmount()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean hasHistoricalOverdue(PaymentSchedule schedule) {
        return schedule.getPaymentStatus() == PaymentScheduleStatus.OVERDUE
                || schedule.getPenaltyAmount().compareTo(BigDecimal.ZERO) > 0
                || schedule.getLastPenaltyAppliedAt() != null;
    }

    private int calculateOnTimeRatioBonus(BigDecimal onTimePaymentRatio) {
        if (onTimePaymentRatio.compareTo(new BigDecimal("0.95")) >= 0) return 40;
        if (onTimePaymentRatio.compareTo(new BigDecimal("0.85")) >= 0) return 20;
        if (onTimePaymentRatio.compareTo(new BigDecimal("0.70")) >= 0) return 5;
        if (onTimePaymentRatio.compareTo(new BigDecimal("0.50")) < 0) return -20;
        return 0;
    }

    private int calculateDebtLoadPenalty(int activeCredits, BigDecimal totalRemainingDebt) {
        int activeCreditsPenalty = activeCredits * 10;
        int debtPenalty = totalRemainingDebt.divide(BigDecimal.valueOf(50_000), 0, RoundingMode.DOWN).multiply(BigDecimal.valueOf(5)).intValue();
        return Math.min(50, activeCreditsPenalty + debtPenalty);
    }

    private String determineRatingLevel(int score) {
        if (score >= 750) return "VERY_HIGH";
        if (score >= 650) return "HIGH";
        if (score >= 500) return "MEDIUM";
        return "LOW";
    }

    private Credit issueCredit(String userId, String bankAccountId, CreditTariff tariff, BigDecimal amount, Integer durationMonths) {
        masterAccountService.reserveFunds(amount);
        try {
            coreServiceClient.applyCredit(bankAccountId, new MoneyOperationDto(amount));
        } catch (Exception e) {
            masterAccountService.releaseFunds(amount);
            throw e;
        }
        BigDecimal monthlyRate = tariff.getInterestRate().divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment = calculateMonthlyPayment(amount, monthlyRate, durationMonths);
        Credit credit = new Credit();
        credit.setOwnerId(userId);
        credit.setBankAccountId(bankAccountId);
        credit.setTariff(tariff);
        credit.setAmount(amount);
        credit.setRemainingAmount(amount);
        credit.setMonthlyPayment(monthlyPayment);
        credit.setDurationMonths(durationMonths);
        credit.setRemainingMonths(durationMonths);
        credit.setStatus(CreditStatus.ACTIVE);
        credit.setIssueDate(LocalDateTime.now());
        credit.setNextPaymentDate(LocalDateTime.now().plusMinutes(1));
        Credit savedCredit = creditRepository.save(credit);
        generatePaymentSchedule(savedCredit, monthlyRate, monthlyPayment);
        refreshCreditState(savedCredit);
        return savedCredit;
    }

    private CreditApplication getPendingApplication(Long applicationId) {
        CreditApplication application = creditApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit application not found with id: " + applicationId));
        if (application.getStatus() != CreditApplicationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit application is already reviewed");
        }
        return application;
    }

    private PaymentProcessingResult applyPaymentToSchedules(Credit credit, BigDecimal amount, PaymentType paymentType, LocalDateTime paymentTime) {
        BigDecimal remainingPayment = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal penaltyPaid = BigDecimal.ZERO;
        BigDecimal interestPaid = BigDecimal.ZERO;
        BigDecimal principalPaid = BigDecimal.ZERO;
        List<PaymentSchedule> schedules = scheduleRepository.findByCreditIdAndPaymentStatusInOrderByPaymentDateAsc(
                credit.getId(), EnumSet.of(PaymentScheduleStatus.OVERDUE, PaymentScheduleStatus.PARTIALLY_PAID, PaymentScheduleStatus.PLANNED)
        );
        for (PaymentSchedule schedule : schedules) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal appliedPenalty = applyAmount(remainingPayment, getRemainingPenalty(schedule), schedule.getPaidPenaltyAmount(), schedule::setPaidPenaltyAmount);
            penaltyPaid = penaltyPaid.add(appliedPenalty);
            remainingPayment = remainingPayment.subtract(appliedPenalty);
            BigDecimal appliedInterest = applyAmount(remainingPayment, getRemainingInterest(schedule), schedule.getPaidInterestAmount(), schedule::setPaidInterestAmount);
            interestPaid = interestPaid.add(appliedInterest);
            remainingPayment = remainingPayment.subtract(appliedInterest);
            BigDecimal appliedPrincipal = applyAmount(remainingPayment, getRemainingPrincipal(schedule), schedule.getPaidPrincipalAmount(), schedule::setPaidPrincipalAmount);
            principalPaid = principalPaid.add(appliedPrincipal);
            remainingPayment = remainingPayment.subtract(appliedPrincipal);
            updateScheduleStatus(schedule, paymentTime);
            scheduleRepository.save(schedule);
        }
        CreditPayment payment = new CreditPayment();
        payment.setCredit(credit);
        payment.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        payment.setPaymentType(paymentType);
        payment.setPaymentDate(paymentTime);
        CreditPayment savedPayment = paymentRepository.save(payment);
        return new PaymentProcessingResult(savedPayment, penaltyPaid, interestPaid, principalPaid);
    }

    private record PaymentProcessingResult(CreditPayment payment, BigDecimal penaltyPaid, BigDecimal interestPaid, BigDecimal principalPaid) {}
}
