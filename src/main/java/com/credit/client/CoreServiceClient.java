package com.credit.client;

import com.credit.config.FeignClientConfig;
import com.credit.dto.MoneyOperationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "core-service",
    url = "${core-service.url}",
    configuration = FeignClientConfig.class
)
public interface CoreServiceClient {

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
