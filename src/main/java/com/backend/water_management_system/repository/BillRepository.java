package com.backend.water_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.MeterReading;

public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByCustomer_SubscriptionNumberOrderByBillDateDesc(String subscriptionNumber);

    Optional<Bill> findByMeterReading(MeterReading meterReading);
}