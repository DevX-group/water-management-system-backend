package com.backend.water_management_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.backend.water_management_system.dto.AddPaymentRequest;
import com.backend.water_management_system.dto.AddPaymentResponse;
import com.backend.water_management_system.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<AddPaymentResponse> addPayment(@RequestBody AddPaymentRequest request) {
        AddPaymentResponse response = paymentService.addPayment(request);
        return ResponseEntity.ok(response);
    }
}
