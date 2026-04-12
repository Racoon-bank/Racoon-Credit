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
    // 3. При ошибке во внешнем core-service повторяем запрос на получение счетов еще несколько раз.
    public List<BankAccountDto> getMyBankAccounts(String authHeader) {
        return coreServiceClient.getMyBankAccounts(authHeader);
    }

    @Retry(name = "coreService")
    // 3. При временной ошибке повторяем попытку зачисления кредита на счет клиента.
    public void applyCredit(String bankAccountId, MoneyOperationDto operation) {
        coreServiceClient.applyCredit(bankAccountId, operation);
    }

    @Retry(name = "coreService")
    // 3. При временной ошибке повторяем попытку списания денег со счета в счет кредита.
    public void payCredit(String bankAccountId, MoneyOperationDto operation) {
        coreServiceClient.payCredit(bankAccountId, operation);
    }
}
