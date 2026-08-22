package com.backend.water_management_system.reports.controller;

import com.backend.water_management_system.reports.dto.BillsSummaryDTO;
import com.backend.water_management_system.reports.entity.BillReport;
import com.backend.water_management_system.reports.service.BillReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills_report")
@CrossOrigin(origins = "*")
public class BillReportController {

    private final BillReportService billService;

    public BillReportController(
            BillReportService billService
    ) {
        this.billService = billService;
    }

    /*
     * Without customerId: returns all bills.
     * With customerId: returns only matching bills.
     */
    @GetMapping
    public ResponseEntity<List<BillReport>> getBills(
            @RequestParam(required = false)
            String customerId
    ) {
        return ResponseEntity.ok(
                billService.getBills(customerId)
        );
    }

    /*
     * Keep this endpoint for compatibility.
     * It performs an exact customer-ID lookup.
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<List<BillReport>> getByCustomer(
            @PathVariable String customerId
    ) {
        return ResponseEntity.ok(
                billService.getBillsByCustomer(customerId)
        );
    }

    @GetMapping("/summary/{customerId}")
    public ResponseEntity<BillsSummaryDTO> getSummary(
            @PathVariable String customerId
    ) {
        return ResponseEntity.ok(
                billService.getSummary(customerId)
        );
    }

    /*
     * Without customerId: returns all overdue bills.
     * With customerId: returns matching overdue bills.
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<BillReport>> getOverdueBills(
            @RequestParam(required = false)
            String customerId
    ) {
        return ResponseEntity.ok(
                billService.getOverdueBills(customerId)
        );
    }
}