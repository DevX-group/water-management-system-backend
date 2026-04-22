package com.backend.water_management_system.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.backend.water_management_system.entity.PaymentAllocation;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, String> {
    List<PaymentAllocation> findByPaymentId(String paymentId);
}
