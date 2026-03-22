package com.credit.repository;

import com.credit.entity.Currency;
import com.credit.entity.MasterAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MasterAccountRepository extends JpaRepository<MasterAccount, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MasterAccount> findByCode(String code);

    Optional<MasterAccount> findByCurrency(Currency currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MasterAccount m where m.currency = :currency")
    Optional<MasterAccount> findLockedByCurrency(@Param("currency") Currency currency);

    List<MasterAccount> findAllByOrderByCurrencyAsc();
}
