package com.backend.water_management_system.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.water_management_system.entity.Customer;
import com.backend.water_management_system.entity.Payment;
import com.backend.water_management_system.entity.PaymentMethod;
import com.backend.water_management_system.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
        List<Payment> findBySubscriptionNumberOrderByCreatedAtDesc(String subscriptionNumber);

        List<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

        Optional<Payment> findByOrderId(String orderId);

        List<Payment> findByStatusAndPaymentMethodAndCreatedAtBefore(PaymentStatus status, PaymentMethod method,
                        LocalDateTime time);

        List<Payment> findBySubscriptionNumberAndStatusInOrderByCreatedAtDesc(String subscriptionNumber,
                        List<PaymentStatus> validStatuses);

        List<Payment> findByPaymentMethodInOrderByCreatedAtDesc(List<PaymentMethod> methods, Pageable pageable);
}
