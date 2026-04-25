package com.backend.water_management_system.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.water_management_system.entity.Bill;
import com.backend.water_management_system.entity.MeterReading;
import com.backend.water_management_system.entity.Customer;

public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByCustomer_SubscriptionNumberOrderByBillDateDesc(String subscriptionNumber);

    Optional<Bill> findByMeterReading(MeterReading meterReading);

    List<Bill> findByCustomer_SubscriptionNumberOrderByBillDateAsc(String subscriptionNumber);

    Optional<Bill> findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(String subscriptionNumber, BigDecimal amount);

    List<Bill> findByCustomer_SubscriptionNumberAndBalanceDueGreaterThanAndBillDateBeforeOrderByBillDateAsc(
        String subscriptionNumber,
        BigDecimal amount,
        LocalDate billDate
    );

    Optional<Bill> findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(String subscriptionNumber);

    Optional<Bill> findTopByCustomerOrderByBillDateDesc(Customer customer);
}
