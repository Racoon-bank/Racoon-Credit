package com.credit.dto;

import com.credit.entity.Currency;
import com.credit.entity.PaymentScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentScheduleResponse {
    private Long id;
    private Long creditId;
    private Currency currency;
    private Integer monthNumber;
    private LocalDateTime paymentDate;
    private BigDecimal totalPayment;
    private BigDecimal interestPayment;
    private BigDecimal principalPayment;
    private BigDecimal remainingBalance;
    private Boolean paid;
    private PaymentScheduleStatus status;
    private BigDecimal penaltyAmount;
    private BigDecimal paidPenaltyAmount;
    private BigDecimal paidInterestAmount;
    private BigDecimal paidPrincipalAmount;
    private Integer overdueDays;
    private LocalDateTime paidAt;
}
