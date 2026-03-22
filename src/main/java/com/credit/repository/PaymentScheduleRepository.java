package com.credit.repository;

import com.credit.entity.PaymentSchedule;
import com.credit.entity.PaymentScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {
    List<PaymentSchedule> findByCreditIdOrderByMonthNumber(Long creditId);
    List<PaymentSchedule> findByCreditIdAndPaymentStatusInOrderByPaymentDateAsc(Long creditId, Collection<PaymentScheduleStatus> statuses);
    List<PaymentSchedule> findByCreditIdAndPaymentStatusOrderByPaymentDateAsc(Long creditId, PaymentScheduleStatus status);
    List<PaymentSchedule> findByPaymentStatusInAndPaymentDateBefore(Collection<PaymentScheduleStatus> statuses, LocalDateTime paymentDate);
    List<PaymentSchedule> findByPaymentStatusOrderByPaymentDateAsc(PaymentScheduleStatus status);
    List<PaymentSchedule> findByCreditOwnerIdAndPaymentStatusOrderByPaymentDateAsc(String ownerId, PaymentScheduleStatus status);
}
