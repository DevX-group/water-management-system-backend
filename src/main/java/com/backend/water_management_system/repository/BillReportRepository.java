package com.backend.water_management_system.repository;

import com.backend.water_management_system.entity.BillReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillReportRepository extends JpaRepository<BillReport, String> {

    List<BillReport> findByCustomerId(String customerId);
}