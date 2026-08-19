package com.backend.water_management_system.controller;

import com.backend.water_management_system.dto.MonthlyReportDTO;
import com.backend.water_management_system.service.CustomerReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
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