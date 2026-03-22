package com.credit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "bank.master-account")
@Data
public class MasterAccountProperties {
    private String codePrefix = "BANK_MASTER_ACCOUNT";
    private String namePrefix = "Bank Master Account";
    private BigDecimal initialBalance = BigDecimal.ZERO;
}
