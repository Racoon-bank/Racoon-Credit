package com.credit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepayCreditRequest {

    @NotNull(message = "ID банковского счета обязателен")
    private String bankAccountId;

    @NotNull(message = "Сумма платежа обязательна")
    @DecimalMin(value = "0.01", message = "Минимальная сумма платежа 0.01")
    private BigDecimal amount;
}
