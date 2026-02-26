package com.credit.service;

import com.credit.entity.Credit;
import com.credit.entity.CreditStatus;
import com.credit.repository.CreditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditSchedulerService {

    private final CreditRepository creditRepository;
    
    private static final BigDecimal OVERDUE_PENALTY_RATE = new BigDecimal("0.1");

    // Проверка просроченных кредитов - выполняется каждый час
    @Scheduled(cron = "0 0 * * * ?") 
    @Transactional
    public void checkOverdueCredits() {
        log.info("Starting overdue credits check");
        
        List<Credit> activeCredits = creditRepository.findByStatus(CreditStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        int overdueCount = 0;

        for (Credit credit : activeCredits) {
            if (credit.getNextPaymentDate() != null && credit.getNextPaymentDate().isBefore(now)) {
                credit.setStatus(CreditStatus.OVERDUE);
                credit.setOverdueDays(1); 
                creditRepository.save(credit);
                overdueCount++;
                
                log.warn("Credit {} is overdue. Payment was due: {}", 
                        credit.getId(), 
                        credit.getNextPaymentDate());
            }
        }
        
        log.info("Overdue credits check completed. Found {} overdue credits", overdueCount);
    }

    // Начисление штрафов за просрочку - выполняется ежедневно в полночь
    @Scheduled(cron = "0 0 0 * * ?") 
    @Transactional
    public void applyOverduePenalties() {
        log.info("Starting overdue penalties application");
        
        List<Credit> overdueCredits = creditRepository.findByStatus(CreditStatus.OVERDUE);
        int penaltiesApplied = 0;

        for (Credit credit : overdueCredits) {
            BigDecimal penalty = credit.getMonthlyPayment()
                    .multiply(OVERDUE_PENALTY_RATE)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            
            BigDecimal newPenalty = credit.getAccumulatedPenalty().add(penalty);
            credit.setAccumulatedPenalty(newPenalty);
            
            credit.setOverdueDays(credit.getOverdueDays() + 1);
            
            creditRepository.save(credit);
            penaltiesApplied++;
            
            log.info("Applied penalty {} to credit {}. Total accumulated penalty: {}, Overdue days: {}", 
                    penalty, credit.getId(), newPenalty, credit.getOverdueDays());
        }
        
        log.info("Overdue penalties application completed. Applied {} penalties", penaltiesApplied);
    }

    // Обновление дат следующих платежей - выполняется ежедневно в 01:00
    @Scheduled(cron = "0 0 1 * * ?") 
    @Transactional
    public void updateNextPaymentDates() {
        log.info("Starting next payment dates update");
        
        List<Credit> activeCredits = creditRepository.findByStatus(CreditStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();

        for (Credit credit : activeCredits) {
            if (credit.getNextPaymentDate() != null && credit.getNextPaymentDate().isBefore(now)) {
                credit.setNextPaymentDate(credit.getNextPaymentDate().plusMonths(1));
                creditRepository.save(credit);
            }
        }
        
        log.info("Next payment dates update completed");
    }
}
