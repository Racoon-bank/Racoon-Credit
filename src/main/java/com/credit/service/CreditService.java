package com.credit.service;

import com.credit.dto.*;
import com.credit.entity.*;
import com.credit.repository.CreditPaymentRepository;
import com.credit.repository.CreditRepository;
import com.credit.repository.CreditTariffRepository;
import com.credit.repository.PaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditService {

    private final CreditRepository creditRepository;
    private final CreditTariffRepository tariffRepository;
    private final CreditPaymentRepository paymentRepository;
    private final PaymentScheduleRepository scheduleRepository;

    @Transactional
    public CreditResponse takeCredit(Long userId, TakeCreditRequest request) {
        log.info("Taking new credit for owner: {}", userId);

        CreditTariff tariff = tariffRepository.findById(request.getTariffId())
                .orElseThrow(() -> new RuntimeException("Tariff not found with id: " + request.getTariffId()));

        
        BigDecimal monthlyRate = tariff.getInterestRate()
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        
        int n = request.getDurationMonths();
        BigDecimal monthlyPayment;
        
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            monthlyPayment = request.getAmount()
                    .divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        } else {
            BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
            BigDecimal onePlusRatePowN = onePlusRate.pow(n);
            
            monthlyPayment = request.getAmount()
                    .multiply(monthlyRate.multiply(onePlusRatePowN))
                    .divide(onePlusRatePowN.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        }

        Credit credit = new Credit();
        credit.setOwnerId(userId);
        credit.setTariff(tariff);
        credit.setAmount(request.getAmount());
        credit.setRemainingAmount(request.getAmount()); 
        credit.setMonthlyPayment(monthlyPayment);
        credit.setDurationMonths(n);
        credit.setRemainingMonths(n);
        credit.setStatus(CreditStatus.ACTIVE);
        credit.setIssueDate(LocalDateTime.now());
        credit.setNextPaymentDate(LocalDateTime.now().plusMinutes(1));

        Credit savedCredit = creditRepository.save(credit);
        log.info("Credit created with id: {}. Monthly payment: {}", savedCredit.getId(), monthlyPayment);

        generatePaymentSchedule(savedCredit, monthlyRate, monthlyPayment);

        return mapToResponse(savedCredit);
    }

    // Погашение кредита с приоритетом: штрафы -> проценты -> основной долг
    @Transactional
    public CreditPaymentResponse repayCredit(Long creditId, RepayCreditRequest request) {
        log.info("Repaying credit {} with amount {}", creditId, request.getAmount());

        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Credit not found with id: " + creditId));

        if (credit.getStatus() != CreditStatus.ACTIVE && credit.getStatus() != CreditStatus.OVERDUE) {
            throw new RuntimeException("Credit cannot be repaid. Current status: " + credit.getStatus());
        }

        BigDecimal remainingPayment = request.getAmount();
        BigDecimal penaltyPaid = BigDecimal.ZERO;
        BigDecimal interestPaid = BigDecimal.ZERO;
        BigDecimal principalPaid = BigDecimal.ZERO;
        
        if (credit.getAccumulatedPenalty().compareTo(BigDecimal.ZERO) > 0) {
            if (remainingPayment.compareTo(credit.getAccumulatedPenalty()) >= 0) {
                penaltyPaid = credit.getAccumulatedPenalty();
                remainingPayment = remainingPayment.subtract(penaltyPaid);
                credit.setAccumulatedPenalty(BigDecimal.ZERO);
                credit.setOverdueDays(0);
            } else {
                penaltyPaid = remainingPayment;
                credit.setAccumulatedPenalty(credit.getAccumulatedPenalty().subtract(penaltyPaid));
                remainingPayment = BigDecimal.ZERO;
            }
        }
        
        if (remainingPayment.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal monthlyRate = credit.getTariff().getInterestRate()
                    .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
            
            BigDecimal interestPayment = credit.getRemainingAmount()
                    .multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);
            
            if (remainingPayment.compareTo(interestPayment) >= 0) {
                interestPaid = interestPayment;
                remainingPayment = remainingPayment.subtract(interestPaid);
            } else {
                interestPaid = remainingPayment;
                remainingPayment = BigDecimal.ZERO;
            }
        }
        
        if (remainingPayment.compareTo(BigDecimal.ZERO) > 0) {
            principalPaid = remainingPayment;
            BigDecimal newRemaining = credit.getRemainingAmount().subtract(principalPaid);
            credit.setRemainingAmount(newRemaining.max(BigDecimal.ZERO));
            
            if (principalPaid.compareTo(BigDecimal.ZERO) > 0 && credit.getRemainingMonths() > 0) {
                credit.setRemainingMonths(credit.getRemainingMonths() - 1);
            }
        }

        CreditPayment payment = new CreditPayment();
        payment.setCredit(credit);
        payment.setAmount(request.getAmount());
        payment.setPaymentType(PaymentType.MANUAL_REPAYMENT);
        payment.setPaymentDate(LocalDateTime.now());
        CreditPayment savedPayment = paymentRepository.save(payment);

        if (credit.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0 || credit.getRemainingMonths() == 0) {
            credit.setStatus(CreditStatus.PAID_OFF);
            credit.setRemainingAmount(BigDecimal.ZERO);
            credit.setRemainingMonths(0);
            credit.setAccumulatedPenalty(BigDecimal.ZERO);
            credit.setOverdueDays(0);
            log.info("Credit {} is fully paid off", creditId);
        } else {
            if (credit.getStatus() == CreditStatus.OVERDUE && credit.getAccumulatedPenalty().compareTo(BigDecimal.ZERO) == 0) {
                credit.setStatus(CreditStatus.ACTIVE);
            }
            credit.setNextPaymentDate(LocalDateTime.now().plusMinutes(1));
            log.info("Credit {} payment processed. Penalty paid: {}, Interest paid: {}, Principal paid: {}",
                    creditId, penaltyPaid, interestPaid, principalPaid);
        }

        creditRepository.save(credit);

        return mapPaymentToResponse(savedPayment);
    }

    // Получение кредита по ID
    @Transactional(readOnly = true)
    public CreditResponse getCreditById(Long id) {
        log.info("Fetching credit with id: {}", id);
        Credit credit = creditRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Credit not found with id: " + id));
        return mapToResponse(credit);
    }

    // Получение списка всех кредитов
    @Transactional(readOnly = true)
    public List<CreditResponse> getAllCredits() {
        log.info("Fetching all credits");
        return creditRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Получение кредитов пользователя
    @Transactional(readOnly = true)
    public List<CreditResponse> getCreditsByUserId(Long userId) {
        log.info("Fetching credits for user: {}", userId);
        return creditRepository.findByOwnerId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Получение истории платежей по кредиту
    @Transactional(readOnly = true)
    public List<CreditPaymentResponse> getCreditPayments(Long creditId) {
        log.info("Fetching payments for credit: {}", creditId);
        return paymentRepository.findByCreditIdOrderByPaymentDateDesc(creditId).stream()
                .map(this::mapPaymentToResponse)
                .collect(Collectors.toList());
    }

    // Расчет статистики по кредиту (общие проценты, переплата)
    @Transactional(readOnly = true)
    public CreditStatisticsResponse getCreditStatistics(Long creditId) {
        log.info("Calculating statistics for credit: {}", creditId);
        
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Credit not found with id: " + creditId));
        
        List<PaymentSchedule> schedule = scheduleRepository.findByCreditIdOrderByMonthNumber(creditId);
        
        BigDecimal totalInterest = schedule.stream()
                .map(PaymentSchedule::getInterestPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalToRepay = credit.getAmount().add(totalInterest);
        
        log.info("Statistics for credit {}: Total interest={}, Total to repay={}", 
                creditId, totalInterest, totalToRepay);
        
        return new CreditStatisticsResponse(
                credit.getId(),
                credit.getAmount(),
                credit.getMonthlyPayment(),
                credit.getDurationMonths(),
                totalToRepay,
                totalInterest,
                credit.getTariff().getInterestRate()
        );
    }

    private CreditResponse mapToResponse(Credit credit) {
        return new CreditResponse(
                credit.getId(),
                credit.getOwnerId(),
                credit.getTariff().getId(),
                credit.getTariff().getName(),
                credit.getTariff().getInterestRate(),
                credit.getAmount(),
                credit.getRemainingAmount(),
                credit.getMonthlyPayment(),
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
        
        log.info("Generating payment schedule for credit {}. Amount: {}, Rate: {}, Payment: {}", 
                credit.getId(), credit.getAmount(), monthlyRate, monthlyPayment);

        for (int month = 1; month <= credit.getDurationMonths(); month++) {
            BigDecimal interestPayment = remainingBalance
                    .multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);

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

            scheduleRepository.save(schedule);

            paymentDate = paymentDate.plusMinutes(1);
            
            log.debug("Month {}: Principal={}, Interest={}, Balance={}", 
                    month, principalPayment, interestPayment, remainingBalance);
        }

        log.info("Payment schedule generated for credit {}. Final balance: {}", credit.getId(), remainingBalance);
    }

    // Получение графика платежей по кредиту
    @Transactional(readOnly = true)
    public List<PaymentScheduleResponse> getPaymentSchedule(Long creditId) {
        log.info("Fetching payment schedule for credit: {}", creditId);
        return scheduleRepository.findByCreditIdOrderByMonthNumber(creditId).stream()
                .map(this::mapScheduleToResponse)
                .collect(Collectors.toList());
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
                schedule.getPaid()
        );
    }
}
