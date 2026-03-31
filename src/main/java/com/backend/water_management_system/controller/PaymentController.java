package com.backend.water_management_system.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.backend.water_management_system.dto.AddPaymentRequest;
import com.backend.water_management_system.dto.AddPaymentResponse;
import com.backend.water_management_system.dto.CustomerPaymentSummaryResponse;
import com.backend.water_management_system.dto.PaymentCustomerInfoResponse;
import com.backend.water_management_system.dto.PaymentHistoryItemResponse;
import com.backend.water_management_system.dto.RecentPaymentResponse;
import com.backend.water_management_system.dto.UpdatePaymentAmountRequest;
import com.backend.water_management_system.service.PaymentService;

@CrossOrigin(origins = "http://localhost:3000")
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

    @GetMapping("/customer/{subscriptionNumber}")
    public ResponseEntity<CustomerPaymentSummaryResponse> getCustomerPaymentSummary(
            @PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(paymentService.getCustomerPaymentSummary(subscriptionNumber));
    }

    @GetMapping("/history/{subscriptionNumber}")
    public ResponseEntity<List<PaymentHistoryItemResponse>> getPaymentHistory(
            @PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(subscriptionNumber));
    }

    @GetMapping("/customerInfo/{subscriptionNumber}")
    public ResponseEntity<PaymentCustomerInfoResponse> getPaymentCustomerInfo(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(paymentService.getPaymentCustomerInfo(subscriptionNumber));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<RecentPaymentResponse>> getRecentPayments(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(paymentService.getRecentPayments(limit));
    }

    @PatchMapping("/{paymentId}")
    public ResponseEntity<AddPaymentResponse> updatePaymentAmount(@PathVariable String paymentId, @RequestBody UpdatePaymentAmountRequest request) {
        AddPaymentResponse response = paymentService.updatePayment(paymentId, request.getAmount());
        return ResponseEntity.ok(response);
    }
}
