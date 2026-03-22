package com.credit.service;

import com.credit.config.MasterAccountProperties;
import com.credit.dto.MasterAccountResponse;
import com.credit.entity.Currency;
import com.credit.entity.MasterAccount;
import com.credit.repository.MasterAccountRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterAccountService {

    private final MasterAccountRepository masterAccountRepository;
    private final MasterAccountProperties masterAccountProperties;

    @PostConstruct
    @Transactional
    public void initializeMasterAccounts() {
        for (Currency currency : Currency.values()) {
            if (masterAccountRepository.findByCurrency(currency).isPresent()) {
                continue;
            }

            MasterAccount account = new MasterAccount();
            account.setCode(buildCode(currency));
            account.setName(buildName(currency));
            account.setCurrency(currency);
            account.setBalance(normalize(masterAccountProperties.getInitialBalance()));
            masterAccountRepository.save(account);
            log.info("Master account {} ({}) initialized with balance {}", account.getCode(), currency, account.getBalance());
        }
    }

    @Transactional(readOnly = true)
    public List<MasterAccountResponse> getMasterAccounts() {
        return masterAccountRepository.findAllByOrderByCurrencyAsc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MasterAccountResponse getMasterAccount(Currency currency) {
        return mapToResponse(loadMasterAccount(currency));
    }

    @Transactional
    public MasterAccountResponse topUp(Currency currency, BigDecimal amount) {
        MasterAccount account = loadMasterAccountForUpdate(currency);
        BigDecimal normalizedAmount = normalize(amount);
        account.setBalance(normalize(account.getBalance().add(normalizedAmount)));
        masterAccountRepository.save(account);
        log.info("Master account {} topped up by {} {}. New balance: {}", account.getCode(), normalizedAmount, currency, account.getBalance());
        return mapToResponse(account);
    }

    @Transactional
    public void reserveFunds(Currency currency, BigDecimal amount) {
        MasterAccount account = loadMasterAccountForUpdate(currency);
        BigDecimal normalizedAmount = normalize(amount);
        if (account.getBalance().compareTo(normalizedAmount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bank master account has insufficient funds for this credit in " + currency
            );
        }

        account.setBalance(normalize(account.getBalance().subtract(normalizedAmount)));
        masterAccountRepository.save(account);
        log.info("Reserved {} {} from master account {}. Remaining balance: {}", normalizedAmount, currency, account.getCode(), account.getBalance());
    }

    @Transactional
    public void releaseFunds(Currency currency, BigDecimal amount) {
        MasterAccount account = loadMasterAccountForUpdate(currency);
        BigDecimal normalizedAmount = normalize(amount);
        account.setBalance(normalize(account.getBalance().add(normalizedAmount)));
        masterAccountRepository.save(account);
        log.info("Released {} {} back to master account {}. Current balance: {}", normalizedAmount, currency, account.getCode(), account.getBalance());
    }

    private MasterAccount loadMasterAccount(Currency currency) {
        return masterAccountRepository.findByCurrency(currency)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Master account is not configured for currency " + currency
                ));
    }

    private MasterAccount loadMasterAccountForUpdate(Currency currency) {
        return masterAccountRepository.findLockedByCurrency(currency)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Master account is not configured for currency " + currency
                ));
    }

    private MasterAccountResponse mapToResponse(MasterAccount account) {
        return new MasterAccountResponse(
                account.getCode(),
                account.getName(),
                account.getCurrency(),
                account.getBalance(),
                account.getUpdatedAt()
        );
    }

    private String buildCode(Currency currency) {
        return masterAccountProperties.getCodePrefix() + "_" + currency.name();
    }

    private String buildName(Currency currency) {
        return masterAccountProperties.getNamePrefix() + " " + currency.name();
    }

    private BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
