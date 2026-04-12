package com.credit.repository;

import com.credit.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    // 2.
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);
}
