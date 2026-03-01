package com.credit.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BankAccountDto {
    private String id;
    private String userId;
    private String accountNumber;
    private Double balance;
}
