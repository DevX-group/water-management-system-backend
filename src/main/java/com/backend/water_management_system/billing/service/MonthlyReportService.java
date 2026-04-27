package com.backend.water_management_system.service;

import com.backend.water_management_system.entity.MonthlyReport;
import com.backend.water_management_system.repository.MonthlyReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service

public class MonthlyReportService {
    @Autowired
    private MonthlyReportRepository repo;

    public List<MonthlyReport> getAllReports() {
        return repo.findAll();
    }

    public List<MonthlyReport> getByYear(int year) {
        return repo.findByYear(year);
    }

}
