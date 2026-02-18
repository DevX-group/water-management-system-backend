package com.backend.water_management_system.repository;

import com.backend.water_management_system.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByCustomer_SubscriptionNumberOrderByBillDateDesc(String subscriptionNumber);
}
