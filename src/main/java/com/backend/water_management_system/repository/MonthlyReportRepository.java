package com.backend.water_management_system.repository;

import com.backend.water_management_system.entity.MonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {
    List<MonthlyReport> findByYear(int year);
}
