package com.credit.repository;

import com.credit.entity.Credit;
import com.credit.entity.CreditStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditRepository extends JpaRepository<Credit, Long> {
    List<Credit> findByStatus(CreditStatus status);
    List<Credit> findByOwnerId(Long ownerId);
}
