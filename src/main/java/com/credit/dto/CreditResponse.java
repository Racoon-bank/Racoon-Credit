package com.credit.dto;

import com.credit.entity.CreditStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditResponse {
    private Long id;
    private String ownerId;
    private Long tariffId;
    private String tariffName;
    private BigDecimal interestRate;
    private BigDecimal amount;
    private BigDecimal remainingAmount;
    private BigDecimal monthlyPayment;
    private BigDecimal totalAmount;
    private Integer durationMonths;
    private Integer remainingMonths;
    private BigDecimal accumulatedPenalty;
    private Integer overdueDays;
    private CreditStatus status;
    private LocalDateTime issueDate;
    private LocalDateTime nextPaymentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
