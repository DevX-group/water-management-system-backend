package com.backend.water_management_system.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.dto.CurrentBillResponse;
import com.backend.water_management_system.dto.CustomerAddPaymentRequest;
import com.backend.water_management_system.dto.CustomerPaymentResponse;
import com.backend.water_management_system.dto.OutstandingBillsSummaryResponse;
import com.backend.water_management_system.dto.PaginationResponse;
import com.backend.water_management_system.dto.PaymentHistoryItemResponse;
import com.backend.water_management_system.service.CustomerPaymentService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequestMapping("/api/customer/payments")
public class CustomerPaymentController {

    private final CustomerPaymentService customerPaymentService;

    public CustomerPaymentController(CustomerPaymentService customerPaymentService) {
        this.customerPaymentService = customerPaymentService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<CustomerPaymentResponse> initiateCustomerPayment(
            @Valid @RequestBody CustomerAddPaymentRequest request) {
        CustomerPaymentResponse response = customerPaymentService.initiateCustomerPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notify")
    public ResponseEntity<String> handlePayhereNotify(@RequestParam Map<String, String> params) {
        customerPaymentService.handlePayhereNotification(params);
        return ResponseEntity.ok("Notification received");
    }

    // Endpoint used by frontend success page to poll latest payment status
    @GetMapping("/status/{orderId}")
    public ResponseEntity<String> getPaymentStatus(@PathVariable String orderId) {
        String status = customerPaymentService.getPaymentStatus(orderId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/history")
    public ResponseEntity<PaginationResponse<PaymentHistoryItemResponse>> getPaymentHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        return ResponseEntity.ok(customerPaymentService.getPaymentHistoryForCustomer(page, size));
    }

    @GetMapping("/current-bill")
    public ResponseEntity<CurrentBillResponse> getCurrentBillForCustomer() {
        CurrentBillResponse response = customerPaymentService.getCurrentBillForCustomer();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/outstanding-bills")
    public ResponseEntity<OutstandingBillsSummaryResponse> getOutstandingBillsForCustomer() {
        OutstandingBillsSummaryResponse response = customerPaymentService.getOutstandingBillsForCustomer();
        return ResponseEntity.ok(response);
    }

}
