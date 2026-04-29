package com.backend.water_management_system.controller;

import com.backend.water_management_system.dto.CustomerReportDTO;
import com.backend.water_management_system.dto.MonthlyReportDTO;
import com.backend.water_management_system.service.CustomerReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports/customer")
@CrossOrigin(origins = "*")
public class CustomerReportController {

    private final CustomerReportService service;

    public CustomerReportController(CustomerReportService service) {
        this.service = service;
    }

    @GetMapping
    public List<CustomerReportDTO> getCustomerReport(
            @RequestParam String customerId,
            @RequestParam int year
    ) {
        return service.getCustomerReport(customerId, year);
    }
}