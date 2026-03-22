package com.credit.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditApplicationDecisionRequest {

    @Size(max = 1000, message = "Комментарий не должен превышать 1000 символов")
    private String comment;
}
