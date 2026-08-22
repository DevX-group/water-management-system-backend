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

        @Query("SELECT b FROM Bill b WHERE b.customer.subscriptionNumber = :subscriptionNumber " +
                        "AND b.balanceDue > 0 " +
                        "AND b.billDate < (SELECT MAX(b2.billDate) FROM Bill b2 WHERE b2.customer.subscriptionNumber = :subscriptionNumber) "
                        +
                        "ORDER BY b.billDate ASC")
        List<Bill> findOutstandingBillsExcludingLatest(String subscriptionNumber);

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

        /** Count of all bills with a given status string. */
        long countByStatus(String status);

        /** Count of all bills with outstanding balance (balanceDue > 0). */
        @Query("SELECT COUNT(b) FROM Bill b WHERE b.balanceDue > 0")
        long countOutstandingBills();

        /** Total outstanding amount across all bills with balance due > 0. */
        @Query("SELECT COALESCE(SUM(b.balanceDue), 0) FROM Bill b WHERE b.balanceDue > 0")
        java.math.BigDecimal sumOutstandingAmount();

        /** Customer-scoped count of pending (unpaid) bills. */
        @Query("SELECT COUNT(b) FROM Bill b WHERE b.customer.subscriptionNumber = :subscriptionNumber AND b.status = 'PENDING'")
        long countPendingBillsBySubscription(@org.springframework.data.repository.query.Param("subscriptionNumber") String subscriptionNumber);
}
