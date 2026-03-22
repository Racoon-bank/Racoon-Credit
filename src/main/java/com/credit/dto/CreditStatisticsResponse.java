package com.credit.dto;

import com.credit.entity.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditStatisticsResponse {
    private Long creditId;
    private Currency currency;
    private BigDecimal originalAmount;
    private BigDecimal monthlyPayment;
    private Integer durationMonths;
    private BigDecimal totalToRepay;
    private BigDecimal totalInterest;
    private BigDecimal interestRate;
}
