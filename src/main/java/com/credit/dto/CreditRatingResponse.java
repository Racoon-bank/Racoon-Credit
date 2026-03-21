package com.credit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditRatingResponse {
    private String userId;
    private Integer score;
    private String ratingLevel;
    private Integer totalCredits;
    private Integer activeCredits;
    private Integer completedCreditsWithoutOverdues;
    private Integer currentOverduePayments;
    private Integer historicalOverduePayments;
    private Integer maxCurrentOverdueDays;
    private BigDecimal onTimePaymentRatio;
    private BigDecimal totalRemainingDebt;
    private LocalDateTime calculatedAt;
}
