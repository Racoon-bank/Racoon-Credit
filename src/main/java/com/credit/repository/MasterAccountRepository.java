package com.credit.repository;

import com.credit.entity.MasterAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MasterAccountRepository extends JpaRepository<MasterAccount, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MasterAccount> findByCode(String code);
}
