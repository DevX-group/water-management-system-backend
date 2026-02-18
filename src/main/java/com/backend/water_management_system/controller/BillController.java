package com.backend.water_management_system.controller;

import com.backend.water_management_system.dto.BillResponse;
import com.backend.water_management_system.service.BillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@CrossOrigin
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @GetMapping("/customer/{subscriptionNumber}")
    public ResponseEntity<List<BillResponse>> getCustomerBills(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(billService.getBillsForCustomer(subscriptionNumber));
    }
}

