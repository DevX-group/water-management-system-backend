package com.backend.water_management_system.customer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.backend.water_management_system.customer.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    @Query("""
                    SELECT c
                    FROM Customer c
                    WHERE LOWER(c.accountHolderName) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(c.subscriptionNumber) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    List<Customer> searchCustomers(@Param("query") String query);

    Optional<Customer> findBySubscriptionNumber(String subscriptionNumber);

    @Query("""
                    SELECT c.user.email
                    FROM Customer c
                    WHERE c.user.email IS NOT NULL
                            AND TRIM(c.user.email) <> ''
            """)
    List<String> findAllCustomerEmails(); // finds all emails of all customers whose email field is not NULL

    @Query("""
                    SELECT c
                    FROM Customer c
                    WHERE c.user.email IS NOT NULL
                            AND TRIM(c.user.email) <> ''
            """)
    List<Customer> findAllCustomersWithEmail();
}
