package com.credit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TakeCreditResultResponse {
    private String resultType;
    private String message;
    private CreditResponse credit;
    private CreditApplicationResponse application;
}
