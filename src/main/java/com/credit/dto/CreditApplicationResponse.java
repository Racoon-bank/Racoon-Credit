package com.credit.dto;

import com.credit.entity.CreditApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditApplicationResponse {
    private Long id;
    private String ownerId;
    private String bankAccountId;
    private Long tariffId;
    private String tariffName;
    private BigDecimal amount;
    private Integer durationMonths;
    private Integer creditRating;
    private CreditApplicationStatus status;
    private String employeeComment;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
