package com.credit.dto;

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
    private Integer monthNumber;
    private LocalDateTime paymentDate;
    private BigDecimal totalPayment;
    private BigDecimal interestPayment;
    private BigDecimal principalPayment;
    private BigDecimal remainingBalance;
    private Boolean paid;
}
