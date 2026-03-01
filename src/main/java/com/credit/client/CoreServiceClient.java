package com.credit.client;

import com.credit.config.FeignClientConfig;
import com.credit.dto.BankAccountDto;
import com.credit.dto.MoneyOperationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(
    name = "core-service",
    url = "${core-service.url}",
    configuration = FeignClientConfig.class
)
public interface CoreServiceClient {

    @GetMapping("/api/bank-accounts/my")
    List<BankAccountDto> getMyBankAccounts(@RequestHeader("Authorization") String authHeader);

    @PutMapping("/internal/bank-accounts/{id}/apply-credit")
    void applyCredit(
        @PathVariable("id") String bankAccountId,
        @RequestBody MoneyOperationDto operation
    );

    @PutMapping("/internal/bank-accounts/{id}/pay-credit")
    void payCredit(
        @PathVariable("id") String bankAccountId,
        @RequestBody MoneyOperationDto operation
    );
}
