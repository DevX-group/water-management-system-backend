package com.backend.water_management_system.payments.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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

        List<Payment> findByPaymentMethodInOrderByCreatedAtDesc(List<PaymentMethod> methods, Pageable pageable);
}
