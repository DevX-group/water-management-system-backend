package com.backend.water_management_system.payments.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.payments.dto.BankDetailsResponse;
import com.backend.water_management_system.payments.service.PaymentConfigService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequestMapping("/api/public/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class PaymentConfigController {
    private final PaymentConfigService paymentConfigService;

    @GetMapping("/bank-details")
    public ResponseEntity<BankDetailsResponse> getBankDetails() {
        return ResponseEntity.ok(paymentConfigService.getBankDetails());
    }
}

