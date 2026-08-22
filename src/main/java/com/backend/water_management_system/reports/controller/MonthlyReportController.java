package com.backend.water_management_system.reports.controller;

import com.backend.water_management_system.reports.dto.MonthlyReportDTO;
import com.backend.water_management_system.reports.service.CustomerReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
public class MonthlyReportController {

    private final CustomerReportService service;

    public MonthlyReportController(CustomerReportService service) {
        this.service = service;
    }

    @GetMapping("/monthly")
    public List<MonthlyReportDTO> getMonthlyReport(@RequestParam int year) {
        return service.getMonthlyReport(year);
    }
}
