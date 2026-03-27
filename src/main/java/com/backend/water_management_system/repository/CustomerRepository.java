package com.backend.water_management_system.repository;

import com.backend.water_management_system.entity.Customer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    @Query("""
            SELECT c FROM Customer c
            WHERE LOWER(c.accountHolderName) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(c.subscriptionNumber) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    List<Customer> searchCustomers(@Param("query") String query);

    @Query("""
            SELECT c.email FROM Customer c
            WHERE c.email IS NOT NULL
              AND TRIM(c.email) <> ''
            """)
    List<String> findAllCustomerEmails();
}
