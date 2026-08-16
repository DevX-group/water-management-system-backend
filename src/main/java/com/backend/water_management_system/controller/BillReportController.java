package com.backend.water_management_system.controller;

import com.backend.water_management_system.dto.BillsSummaryDTO;
import com.backend.water_management_system.entity.BillReport;
import com.backend.water_management_system.service.BillReportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/bills_report")
@CrossOrigin(origins = "*")
public class BillReportController {

    private final BillReportService billService;

    public BillReportController(BillReportService billService) {
        this.billService = billService;
    }

    // ALL bills
    @GetMapping
    public ResponseEntity<?> getAllBills() {
        try {
            List<BillReport> result = billService.getAllBills();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch bills: " + e.getMessage());
        }
    }

    // BY customer
    @GetMapping("/{customerId}")
    public ResponseEntity<?> getByCustomer(@PathVariable String customerId) {
        try {
            List<BillReport> result = billService.getBillsByCustomer(customerId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch customer bills: " + e.getMessage());
        }
    }

    // SUMMARY
    @GetMapping("/summary/{customerId}")
    public ResponseEntity<?> getSummary(@PathVariable String customerId) {
        try {
            BillsSummaryDTO result = billService.getSummary(customerId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch summary: " + e.getMessage());
        }
    }

    // OVERDUE BILLS
    @GetMapping("/overdue")
    public ResponseEntity<?> getOverdueBills() {
        try {
            List<BillReport> result = billService.getOverdueBills();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch overdue bills: " + e.getMessage());
        }
    }
}