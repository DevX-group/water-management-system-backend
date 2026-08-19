package com.backend.water_management_system.reports.repository;

import com.backend.water_management_system.reports.entity.BillReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BillReportRepository extends JpaRepository<BillReport, String> {

    // ✅ FIX: used in service
    List<BillReport> findByCustomerId(String customerId);

    // Overdue bills (JPQL version)
    @Query("SELECT b FROM BillReport b WHERE b.status = 'UNPAID' AND b.dueDate < :today")
    List<BillReport> findOverdueBills(@Param("today") LocalDate today);
}
