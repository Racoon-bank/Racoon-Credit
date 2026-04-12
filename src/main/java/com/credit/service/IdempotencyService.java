package com.credit.service;

import com.credit.entity.IdempotencyRecord;
import com.credit.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> get(String key) {
        return idempotencyRecordRepository.findByIdempotencyKey(key);
    }

    @Transactional
    public void save(String key, String responseBody, int statusCode) {
        // 2. Повторно ту же запись не создаем, чтобы один idempotency key соответствовал одному результату.
        if (idempotencyRecordRepository.findByIdempotencyKey(key).isPresent()) {
            return;
        }

        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setResponseBody(responseBody);
        record.setStatusCode(statusCode);
        idempotencyRecordRepository.save(record);
    }
}
