package com.credit.repository;

import com.credit.entity.CreditApplication;
import com.credit.entity.CreditApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditApplicationRepository extends JpaRepository<CreditApplication, Long> {
    List<CreditApplication> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
    List<CreditApplication> findByStatusOrderByCreatedAtAsc(CreditApplicationStatus status);
}
