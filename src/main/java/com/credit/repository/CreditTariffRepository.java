package com.credit.repository;

import com.credit.entity.CreditTariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditTariffRepository extends JpaRepository<CreditTariff, Long> {
    Optional<CreditTariff> findByName(String name);
    boolean existsByName(String name);
}
