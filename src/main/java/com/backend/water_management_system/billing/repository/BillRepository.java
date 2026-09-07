package com.backend.water_management_system.billing.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.backend.water_management_system.billing.entity.Bill;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.meter_reading.entity.MeterReading;

public interface BillRepository extends JpaRepository<Bill, Long> {

        List<Bill> findByCustomer_SubscriptionNumberOrderByBillDateDesc(String subscriptionNumber);
        
        Optional<Bill> findByShareToken(String shareToken);
        
        @Query("SELECT b FROM Bill b LEFT JOIN b.customer c LEFT JOIN c.user u " +
               "WHERE LOWER(c.accountHolderName) LIKE LOWER(CONCAT('%', :query, '%')) " +
               "OR LOWER(u.nic) LIKE LOWER(CONCAT('%', :query, '%')) " +
               "OR LOWER(c.subscriptionNumber) LIKE LOWER(CONCAT('%', :query, '%')) " +
               "OR (CAST(b.billId AS string) = :query) " +
               "OR (CONCAT('n-', CAST(b.billId AS string)) = LOWER(:query)) " +
               "ORDER BY b.billDate DESC")
        List<Bill> searchBills(@org.springframework.data.repository.query.Param("query") String query);

        org.springframework.data.domain.Page<Bill> findByCustomer_SubscriptionNumberOrderByBillDateDesc(String subscriptionNumber, org.springframework.data.domain.Pageable pageable);

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
        @Query("""
                        SELECT COALESCE(SUM(b.balanceDue), 0)
                        FROM Bill b
                        WHERE b.customer.subscriptionNumber = :subscriptionNumber
                        AND b.status = 'PENDING'
                        """)
        BigDecimal getTotalPendingBalance(String subscriptionNumber);
        long countByStatus(String status);
        @Query("SELECT COUNT(b) FROM Bill b WHERE b.balanceDue > 0")
        long countOutstandingBills();
        @Query("SELECT COALESCE(SUM(b.balanceDue), 0) FROM Bill b WHERE b.balanceDue > 0")
        java.math.BigDecimal sumOutstandingAmount();
        @Query("SELECT COUNT(b) FROM Bill b WHERE b.customer.subscriptionNumber = :subscriptionNumber AND b.status = 'PENDING'")
        long countPendingBillsBySubscription(@org.springframework.data.repository.query.Param("subscriptionNumber") String subscriptionNumber);
}
