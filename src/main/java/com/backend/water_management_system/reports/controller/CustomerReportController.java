package com.backend.water_management_system.reports.controller;

import com.backend.water_management_system.reports.dto.CustomerReportDTO;
import com.backend.water_management_system.reports.service.CustomerReportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

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
    public ResponseEntity<?> getCustomerReport(
            @RequestParam String customerId,
            @RequestParam int year
    ) {
        try {
            List<CustomerReportDTO> result =
                    service.getCustomerReport(customerId, year);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate customer report: " + e.getMessage());
        }
    }
}
