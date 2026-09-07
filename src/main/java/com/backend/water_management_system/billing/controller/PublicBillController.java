package com.backend.water_management_system.billing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.billing.dto.BillResponse;
import com.backend.water_management_system.billing.service.BillService;

@RestController
@RequestMapping("/api/public/bills")
public class PublicBillController {

    private final BillService billService;

    public PublicBillController(BillService billService) {
        this.billService = billService;
    }

    @GetMapping("/share/{token}")
    public ResponseEntity<BillResponse> getBillByToken(@PathVariable String token) {
        BillResponse bill = billService.getBillByShareToken(token);
        return ResponseEntity.ok(bill);
    }
}
