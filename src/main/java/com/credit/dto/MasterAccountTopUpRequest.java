package com.credit.dto;

import com.credit.entity.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasterAccountTopUpRequest {

    @NotNull(message = "Валюта обязательна")
    private Currency currency;

    @NotNull(message = "Сумма пополнения обязательна")
    @DecimalMin(value = "0.01", message = "Сумма пополнения должна быть больше 0")
    private BigDecimal amount;
}
