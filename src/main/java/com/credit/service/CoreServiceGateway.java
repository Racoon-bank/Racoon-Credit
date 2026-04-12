package com.credit.service;

import com.credit.client.CoreServiceClient;
import com.credit.dto.BankAccountDto;
import com.credit.dto.MoneyOperationDto;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoreServiceGateway {

    private final CoreServiceClient coreServiceClient;

    @Retry(name = "coreService")
    // 3.
    public List<BankAccountDto> getMyBankAccounts(String authHeader) {
        return coreServiceClient.getMyBankAccounts(authHeader);
    }

    @Retry(name = "coreService")
    // 3.
    public void applyCredit(String bankAccountId, MoneyOperationDto operation) {
        coreServiceClient.applyCredit(bankAccountId, operation);
    }

    @Retry(name = "coreService")
    // 3.
    public void payCredit(String bankAccountId, MoneyOperationDto operation) {
        coreServiceClient.payCredit(bankAccountId, operation);
    }
}
