package com.backend.water_management_system.payments.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.payments.dto.BankDetailsResponse;
import com.backend.water_management_system.payments.service.PaymentConfigService;

@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequestMapping("/api/public/payments")
public class PaymentConfigController {
    private final PaymentConfigService paymentConfigService;

    public PaymentConfigController(PaymentConfigService paymentConfigService) {
        this.paymentConfigService = paymentConfigService;
    }

    @GetMapping("/bank-details")
    public ResponseEntity<BankDetailsResponse> getBankDetails() {
        return ResponseEntity.ok(paymentConfigService.getBankDetails());
    }
}

