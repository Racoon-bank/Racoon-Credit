package com.credit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditTariffRequest {

    @NotBlank(message = "Название тарифа обязательно")
    private String name;

    @NotNull(message = "Процентная ставка обязательна")
    @DecimalMin(value = "0.01", message = "Процентная ставка должна быть больше 0")
    @jakarta.validation.constraints.DecimalMax(value = "100", message = "Процентная ставка не может превышать 100%")
    private BigDecimal interestRate; // в процентах

    @NotNull(message = "Дата окончания действия тарифа обязательна")
    private LocalDate dueDate;

    private Boolean isActive = true;
}
