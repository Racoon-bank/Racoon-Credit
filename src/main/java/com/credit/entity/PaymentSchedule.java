package com.credit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_id", nullable = false)
    private Credit credit;

    @Column(nullable = false)
    private Integer monthNumber;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPayment;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interestPayment;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalPayment;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingBalance;

    @Column(nullable = false)
    private Boolean paid = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentScheduleStatus paymentStatus = PaymentScheduleStatus.PLANNED;

    @Column(name = "paid_penalty_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidPenaltyAmount = BigDecimal.ZERO;

    @Column(name = "paid_interest_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidInterestAmount = BigDecimal.ZERO;

    @Column(name = "paid_principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidPrincipalAmount = BigDecimal.ZERO;

    @Column(name = "penalty_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "overdue_days", nullable = false)
    private Integer overdueDays = 0;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "last_penalty_applied_at")
    private LocalDateTime lastPenaltyAppliedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (paymentStatus == null) {
            paymentStatus = PaymentScheduleStatus.PLANNED;
        }
        if (paidPenaltyAmount == null) {
            paidPenaltyAmount = BigDecimal.ZERO;
        }
        if (paidInterestAmount == null) {
            paidInterestAmount = BigDecimal.ZERO;
        }
        if (paidPrincipalAmount == null) {
            paidPrincipalAmount = BigDecimal.ZERO;
        }
        if (penaltyAmount == null) {
            penaltyAmount = BigDecimal.ZERO;
        }
        if (overdueDays == null) {
            overdueDays = 0;
        }
    }
}
