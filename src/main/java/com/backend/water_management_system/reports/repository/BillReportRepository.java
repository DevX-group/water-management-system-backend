package com.backend.water_management_system.reports.repository;

import com.backend.water_management_system.reports.entity.BillReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BillReportRepository
        extends JpaRepository<BillReport, String> {

    // Exact customer lookup used by customer summary
    List<BillReport> findByCustomerId(String customerId);

    // Bills report search performed by database
    @Query("""
        SELECT b
        FROM BillReport b
        WHERE :customerId IS NULL
           OR :customerId = ''
           OR UPPER(b.customerId) LIKE
              UPPER(CONCAT('%', :customerId, '%'))
        ORDER BY b.billReportDate DESC
        """)
    List<BillReport> findFilteredBills(
            @Param("customerId") String customerId
    );

    // Overdue report filtered by database
    @Query("""
        SELECT b
        FROM BillReport b
        WHERE UPPER(b.status) = 'UNPAID'
          AND b.dueDate < :today
          AND (
              :customerId IS NULL
              OR :customerId = ''
              OR UPPER(b.customerId) LIKE
                 UPPER(CONCAT('%', :customerId, '%'))
          )
        ORDER BY b.dueDate
        """)
    List<BillReport> findOverdueBills(
            @Param("today") LocalDate today,
            @Param("customerId") String customerId
    );
}