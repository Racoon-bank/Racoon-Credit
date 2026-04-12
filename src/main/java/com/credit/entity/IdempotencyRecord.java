package com.credit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    // 2. Ключ, по которому понимаем, что запрос уже был обработан раньше.
    private String idempotencyKey;

    @Column(name = "response_body", nullable = false, columnDefinition = "text")
    // 2. Сохраняем исходный ответ, чтобы отдать его повторно без повторного запуска бизнес-логики.
    private String responseBody;

    @Column(name = "status_code", nullable = false)
    // 2. Вместе с телом ответа сохраняем и HTTP-статус.
    private Integer statusCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
