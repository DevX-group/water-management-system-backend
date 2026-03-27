package com.backend.water_management_system.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.backend.water_management_system.entity.Payment;
import java.util.List;
public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findBySubscriptionNumberOrderByCreatedAtDesc(String subscriptionNumber);

    List<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
