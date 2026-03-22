package com.credit.dto;

import com.credit.entity.PaymentScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverduePaymentResponse {
    private Long scheduleId;
    private Long creditId;
    private Integer monthNumber;
    private LocalDateTime paymentDate;
    private BigDecimal totalPayment;
    private BigDecimal paidAmount;
    private BigDecimal remainingDue;
    private BigDecimal interestPayment;
    private BigDecimal principalPayment;
    private BigDecimal penaltyAmount;
    private BigDecimal paidPenaltyAmount;
    private BigDecimal paidInterestAmount;
    private BigDecimal paidPrincipalAmount;
    private Integer overdueDays;
    private PaymentScheduleStatus status;
}
