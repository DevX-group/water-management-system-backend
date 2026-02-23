package com.backend.water_management_system.controller;

import com.backend.water_management_system.dto.BillResponse;
import com.backend.water_management_system.dto.CurrentBillResponse;
import com.backend.water_management_system.dto.OutstandingBillItemResponse;
import com.backend.water_management_system.service.BillService;
import com.backend.water_management_system.service.PaymentService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@CrossOrigin(origins = "http://localhost:3000")

public class BillController {

    private final BillService billService;
    private final PaymentService paymentService;

    public BillController(BillService billService, PaymentService paymentService) {
        this.billService = billService;
        this.paymentService = paymentService;
    }

    @GetMapping("/customer/{subscriptionNumber}")
    public ResponseEntity<List<BillResponse>> getCustomerBills(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(billService.getBillsForCustomer(subscriptionNumber));
    }

    @GetMapping("/current/{subscriptionNumber}")
    public ResponseEntity<CurrentBillResponse> getCurrentBill(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(paymentService.getCurrentBill(subscriptionNumber));
    }

    @GetMapping("/outstanding/{subscriptionNumber}")
    public ResponseEntity<List<OutstandingBillItemResponse>> getOutstandingBills(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(paymentService.getOutstandingBills(subscriptionNumber));
    }
}

