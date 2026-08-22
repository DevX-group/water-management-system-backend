package com.backend.water_management_system.payments.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.backend.water_management_system.common.dto.PaginationResponse;
import com.backend.water_management_system.payments.dto.AddPaymentRequest;
import com.backend.water_management_system.payments.dto.AddPaymentResponse;
import com.backend.water_management_system.payments.dto.CustomerPaymentSummaryResponse;
import com.backend.water_management_system.payments.dto.PaymentCustomerInfoResponse;
import com.backend.water_management_system.payments.dto.PaymentHistoryItemResponse;
import com.backend.water_management_system.payments.dto.RecentPaymentResponse;
import com.backend.water_management_system.payments.dto.UpdatePaymentAmountRequest;
import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.payments.service.PaymentService;

@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN') or hasRole('CUSTOMER_HANDLER')")
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
    public ResponseEntity<PaginationResponse<PaymentHistoryItemResponse>> getPaymentHistory(
            @PathVariable String subscriptionNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PaymentMethod paymentMethod) {

        return ResponseEntity.ok(paymentService.getPaymentHistory(subscriptionNumber, page, size, year, paymentMethod));
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
    public ResponseEntity<AddPaymentResponse> updatePaymentAmount(@PathVariable String paymentId,
            @RequestBody UpdatePaymentAmountRequest request) {
        AddPaymentResponse response = paymentService.updatePayment(paymentId, request.getAmount());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{paymentId}")
    public ResponseEntity<Void> deletePayment(@PathVariable String paymentId) {
        paymentService.deletePayment(paymentId);
        return ResponseEntity.noContent().build();
    }
}
