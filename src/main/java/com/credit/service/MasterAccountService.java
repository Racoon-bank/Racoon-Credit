package com.credit.service;

import com.credit.config.MasterAccountProperties;
import com.credit.dto.MasterAccountResponse;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterAccountService {

    private final MasterAccountRepository masterAccountRepository;
    private final MasterAccountProperties masterAccountProperties;

    @PostConstruct
    @Transactional
    public void initializeMasterAccount() {
        if (masterAccountRepository.existsById(masterAccountProperties.getCode())) {
            return;
        }

        MasterAccount account = new MasterAccount();
        account.setCode(masterAccountProperties.getCode());
        account.setName(masterAccountProperties.getName());
        account.setCurrency(masterAccountProperties.getCurrency());
        account.setBalance(masterAccountProperties.getInitialBalance().setScale(2, RoundingMode.HALF_UP));
        masterAccountRepository.save(account);
        log.info("Master account {} initialized with balance {}", account.getCode(), account.getBalance());
    }

    @Transactional(readOnly = true)
    public MasterAccountResponse getMasterAccount() {
        return mapToResponse(loadMasterAccount());
    }

    @Transactional
    public MasterAccountResponse topUp(BigDecimal amount) {
        MasterAccount account = loadMasterAccountForUpdate();
        account.setBalance(account.getBalance().add(amount).setScale(2, RoundingMode.HALF_UP));
        masterAccountRepository.save(account);
        log.info("Master account {} topped up by {}. New balance: {}", account.getCode(), amount, account.getBalance());
        return mapToResponse(account);
    }

    @Transactional
    public void reserveFunds(BigDecimal amount) {
        MasterAccount account = loadMasterAccountForUpdate();
        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);
        if (account.getBalance().compareTo(normalizedAmount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bank master account has insufficient funds for this credit"
            );
        }

        account.setBalance(account.getBalance().subtract(normalizedAmount).setScale(2, RoundingMode.HALF_UP));
        masterAccountRepository.save(account);
        log.info("Reserved {} from master account {}. Remaining balance: {}", normalizedAmount, account.getCode(), account.getBalance());
    }

    @Transactional
    public void releaseFunds(BigDecimal amount) {
        MasterAccount account = loadMasterAccountForUpdate();
        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);
        account.setBalance(account.getBalance().add(normalizedAmount).setScale(2, RoundingMode.HALF_UP));
        masterAccountRepository.save(account);
        log.info("Released {} back to master account {}. Current balance: {}", normalizedAmount, account.getCode(), account.getBalance());
    }

    private MasterAccount loadMasterAccount() {
        return masterAccountRepository.findById(masterAccountProperties.getCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Master account is not configured"));
    }

    private MasterAccount loadMasterAccountForUpdate() {
        return masterAccountRepository.findByCode(masterAccountProperties.getCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Master account is not configured"));
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
}
