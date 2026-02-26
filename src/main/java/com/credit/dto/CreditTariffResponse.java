package com.credit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditTariffResponse {
    private Long id;
    private String name;
    private BigDecimal interestRate;
    private LocalDateTime createdAt;
}
