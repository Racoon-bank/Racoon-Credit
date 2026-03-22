package com.credit.dto;

import com.credit.entity.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasterAccountResponse {
    private String code;
    private String name;
    private Currency currency;
    private BigDecimal balance;
    private LocalDateTime updatedAt;
}
