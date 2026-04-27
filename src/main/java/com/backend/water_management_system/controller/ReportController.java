package com.backend.water_management_system.controller;

import com.backend.water_management_system.entity.MonthlyReport;
import com.backend.water_management_system.service.MonthlyReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private MonthlyReportService service;

    @GetMapping("/monthly")
    public List<MonthlyReport> getMonthly(@RequestParam(required = false) Integer year) {
        if (year != null) {
            return service.getByYear(year);
        }
        return service.getAllReports();
    }
}