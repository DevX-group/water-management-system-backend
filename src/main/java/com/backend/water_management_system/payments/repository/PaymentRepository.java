package com.backend.water_management_system.payments.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.backend.water_management_system.payments.entity.Payment;
import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.payments.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
        List<Payment> findBySubscriptionNumberOrderByCreatedAtDesc(String subscriptionNumber);

        List<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

        Optional<Payment> findByOrderId(String orderId);

        List<Payment> findByStatusAndPaymentMethodAndCreatedAtBefore(PaymentStatus status, PaymentMethod method,
                        LocalDateTime time);

        Page<Payment> findBySubscriptionNumberAndStatusInOrderByCreatedAtDesc(String subscriptionNumber,
                        List<PaymentStatus> validStatuses, Pageable pageable);

        @Query("""
            SELECT p FROM Payment p
            WHERE p.subscriptionNumber = :subscriptionNumber
            AND p.status IN :validStatuses
            AND (:year IS NULL OR YEAR(p.createdAt) = :year)
            AND (:paymentMethod IS NULL OR p.paymentMethod = :paymentMethod)
        """)
        Page<Payment> findBySubscriptionNumberAndFilters(
                @Param("subscriptionNumber") String subscriptionNumber,
                @Param("validStatuses") List<PaymentStatus> validStatuses,
                @Param("year") Integer year,
                @Param("paymentMethod") PaymentMethod paymentMethod,
                Pageable pageable);

        List<Payment> findByPaymentMethodInOrderByCreatedAtDesc(List<PaymentMethod> methods, Pageable pageable);

        /** Sum of payment amounts completed in the current month (status COMPLETED). */
        @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'COMPLETED' AND YEAR(p.createdAt) = :year AND MONTH(p.createdAt) = :month")
        java.math.BigDecimal sumCompletedPaymentsByMonth(@org.springframework.data.repository.query.Param("year") int year, @org.springframework.data.repository.query.Param("month") int month);
}
