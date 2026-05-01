package com.backend.water_management_system.controller;

import com.backend.water_management_system.dto.BillsSummaryDTO;
import com.backend.water_management_system.entity.BillReport;
import com.backend.water_management_system.service.BillReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills_report")
@CrossOrigin(origins = "*")
public class BillReportController {

    private final BillReportService billService;

    public BillReportController(BillReportService billService) {
        this.billService = billService;
    }

    @GetMapping
    public List<BillReport> getAllBills() {
        return billService.getAllBills();
    }

    @GetMapping("/{customerId}")
    public List<BillReport> getByCustomer(@PathVariable String customerId) {
        return billService.getBillsByCustomer(customerId);
    }

    @GetMapping("/summary/{customerId}")
    public BillsSummaryDTO getSummary(@PathVariable String customerId) {
        return billService.getSummary(customerId);
    }
}