package com.backend.water_management_system.billing.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.meter_reading.entity.MeterReading;
import com.backend.water_management_system.customer.entity.Customer;

public interface BillRepository extends JpaRepository<Bill, Long> {

        List<Bill> findByCustomer_SubscriptionNumberOrderByBillDateDesc(String subscriptionNumber);

        Optional<Bill> findByMeterReading(MeterReading meterReading);

        List<Bill> findByCustomer_SubscriptionNumberOrderByBillDateAsc(String subscriptionNumber);

        Optional<Bill> findTopByCustomer_SubscriptionNumberAndBalanceDueGreaterThanOrderByBillDateDesc(
                        String subscriptionNumber, BigDecimal amount);

        List<Bill> findByCustomer_SubscriptionNumberAndBalanceDueGreaterThanAndBillDateBeforeOrderByBillDateAsc(
                        String subscriptionNumber,
                        BigDecimal amount,
                        LocalDate billDate);

        Optional<Bill> findTopByCustomer_SubscriptionNumberOrderByBillDateDesc(String subscriptionNumber);

        Optional<Bill> findTopByCustomerOrderByBillDateDesc(Customer customer);

        Optional<Bill> findTopByCustomerSubscriptionNumberOrderByBillDateDesc(String subscriptionNumber);

        List<Bill> findByCustomerSubscriptionNumberAndStatusOrderByGeneratedAtAsc(
                        String subscriptionNumber,
                        String status);

        /**
         * Returns the total unpaid balance of all PENDING bills for a given customer.
         * This includes both current month charges and any outstanding amounts from
         * previous billing cycles.
         */
        @Query("""
                        SELECT COALESCE(SUM(b.balanceDue), 0)
                        FROM Bill b
                        WHERE b.customer.subscriptionNumber = :subscriptionNumber
                        AND b.status = 'PENDING'
                        """)
        BigDecimal getTotalPendingBalance(String subscriptionNumber);
}
