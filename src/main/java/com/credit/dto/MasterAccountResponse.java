package com.credit.dto;

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
    private String currency;
    private BigDecimal balance;
    private LocalDateTime updatedAt;
}
