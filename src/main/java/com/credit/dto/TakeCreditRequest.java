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
public class TakeCreditRequest {

    @NotNull(message = "Имя владельца обязательно")
    private String ownerName;

    @NotNull(message = "ID тарифа обязателен")
    private Long tariffId;

    @NotNull(message = "Сумма кредита обязательна")
    @DecimalMin(value = "1000.0", message = "Минимальная сумма кредита 1000")
    private BigDecimal amount;

    @NotNull(message = "Количество месяцев обязательно")
    @DecimalMin(value = "1", message = "Минимальный срок 1 месяц")
    private Integer durationMonths;
}
