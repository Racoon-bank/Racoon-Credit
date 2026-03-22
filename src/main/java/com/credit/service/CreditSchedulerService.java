package com.credit.service;

import com.credit.entity.Credit;
import com.credit.entity.CreditStatus;
import com.credit.entity.CreditTariff;
import com.credit.entity.PaymentSchedule;
import com.credit.entity.PaymentScheduleStatus;
import com.credit.repository.CreditRepository;
import com.credit.repository.CreditTariffRepository;
import com.credit.repository.PaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditSchedulerService {

    private final CreditRepository creditRepository;
    private final CreditTariffRepository creditTariffRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final CreditService creditService;

    private static final BigDecimal OVERDUE_PENALTY_RATE = new BigDecimal("0.1");

    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void checkOverdueCredits() {
        log.info("Starting overdue credits check");

        LocalDateTime now = LocalDateTime.now();
        List<PaymentSchedule> dueSchedules = paymentScheduleRepository.findByPaymentStatusInAndPaymentDateBefore(
                EnumSet.of(PaymentScheduleStatus.PLANNED, PaymentScheduleStatus.PARTIALLY_PAID, PaymentScheduleStatus.OVERDUE),
                now
        );
        int overdueCount = 0;

        for (PaymentSchedule schedule : dueSchedules) {
            if (Boolean.TRUE.equals(schedule.getPaid())) {
                continue;
            }

            if (creditService.tryAutomaticRepayment(schedule.getCredit(), schedule)) {
                continue;
            }

            int overdueDays = Math.max(1, (int) ChronoUnit.DAYS.between(schedule.getPaymentDate().toLocalDate(), now.toLocalDate()));
            schedule.setOverdueDays(overdueDays);
            schedule.setPaymentStatus(PaymentScheduleStatus.OVERDUE);
            paymentScheduleRepository.save(schedule);
            overdueCount++;

            log.warn("Payment schedule {} for credit {} is overdue. Payment was due: {}",
                    schedule.getId(),
                    schedule.getCredit().getId(),
                    schedule.getPaymentDate());
        }

        updateCreditAggregates();
        log.info("Overdue credits check completed. Found {} overdue schedules", overdueCount);
    }

    // Начисление штрафов за просрочку - выполняется каждую минуту
    @Scheduled(cron = "0 * * * * ?") 
    @Transactional
    public void applyOverduePenalties() {
        log.info("Starting overdue penalties application");

        List<PaymentSchedule> overdueSchedules = paymentScheduleRepository.findByPaymentStatusOrderByPaymentDateAsc(PaymentScheduleStatus.OVERDUE);
        LocalDateTime now = LocalDateTime.now();
        int penaltiesApplied = 0;

        for (PaymentSchedule schedule : overdueSchedules) {
            if (schedule.getLastPenaltyAppliedAt() != null
                    && !schedule.getLastPenaltyAppliedAt().toLocalDate().isBefore(now.toLocalDate())) {
                continue;
            }

            BigDecimal remainingDue = schedule.getTotalPayment()
                    .add(schedule.getPenaltyAmount())
                    .subtract(schedule.getPaidPenaltyAmount())
                    .subtract(schedule.getPaidInterestAmount())
                    .subtract(schedule.getPaidPrincipalAmount());
            if (remainingDue.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal penalty = remainingDue
                    .multiply(OVERDUE_PENALTY_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
            schedule.setPenaltyAmount(schedule.getPenaltyAmount().add(penalty));
            schedule.setLastPenaltyAppliedAt(now);
            paymentScheduleRepository.save(schedule);
            penaltiesApplied++;

            log.info("Applied penalty {} to schedule {} of credit {}. Total penalty: {}",
                    penalty,
                    schedule.getId(),
                    schedule.getCredit().getId(),
                    schedule.getPenaltyAmount());
        }

        updateCreditAggregates();
        log.info("Overdue penalties application completed. Applied {} penalties", penaltiesApplied);
    }

    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void updateNextPaymentDates() {
        log.info("Starting next payment dates update");
        updateCreditAggregates();
        log.info("Next payment dates update completed");
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void deactivateExpiredTariffs() {
        log.info("Starting expired tariffs deactivation");

        List<CreditTariff> activeTariffs = creditTariffRepository.findAll().stream()
                .filter(tariff -> tariff.getIsActive() && tariff.getDueDate().isBefore(LocalDate.now()))
                .toList();

        int deactivatedCount = 0;

        for (CreditTariff tariff : activeTariffs) {
            tariff.setIsActive(false);
            creditTariffRepository.save(tariff);
            deactivatedCount++;

            log.info("Tariff {} '{}' deactivated. Due date was: {}",
                    tariff.getId(),
                    tariff.getName(),
                    tariff.getDueDate());
        }

        log.info("Expired tariffs deactivation completed. Deactivated {} tariffs", deactivatedCount);
    }

    private void updateCreditAggregates() {
        List<Credit> credits = creditRepository.findAll();
        for (Credit credit : credits) {
            List<PaymentSchedule> schedules = paymentScheduleRepository.findByCreditIdOrderByMonthNumber(credit.getId());

            BigDecimal remainingPrincipal = schedules.stream()
                    .map(schedule -> schedule.getPrincipalPayment().subtract(schedule.getPaidPrincipalAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal outstandingPenalty = schedules.stream()
                    .map(schedule -> schedule.getPenaltyAmount().subtract(schedule.getPaidPenaltyAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            int remainingMonths = (int) schedules.stream()
                    .filter(schedule -> schedule.getPaymentStatus() != PaymentScheduleStatus.PAID)
                    .count();
            int maxOverdueDays = schedules.stream()
                    .filter(schedule -> schedule.getPaymentStatus() == PaymentScheduleStatus.OVERDUE)
                    .map(PaymentSchedule::getOverdueDays)
                    .max(Integer::compareTo)
                    .orElse(0);

            credit.setRemainingAmount(remainingPrincipal);
            credit.setAccumulatedPenalty(outstandingPenalty);
            credit.setRemainingMonths(remainingMonths);
            credit.setOverdueDays(maxOverdueDays);
            credit.setNextPaymentDate(schedules.stream()
                    .filter(schedule -> schedule.getPaymentStatus() != PaymentScheduleStatus.PAID)
                    .map(PaymentSchedule::getPaymentDate)
                    .min(LocalDateTime::compareTo)
                    .orElse(null));

            if (remainingMonths == 0 && remainingPrincipal.compareTo(BigDecimal.ZERO) <= 0 && outstandingPenalty.compareTo(BigDecimal.ZERO) <= 0) {
                credit.setStatus(CreditStatus.PAID_OFF);
            } else if (maxOverdueDays > 0) {
                credit.setStatus(CreditStatus.OVERDUE);
            } else {
                credit.setStatus(CreditStatus.ACTIVE);
            }

            creditRepository.save(credit);
        }
    }
}
