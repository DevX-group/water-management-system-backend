package com.backend.water_management_system.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.dto.AddPaymentRequest;
import com.backend.water_management_system.dto.CustomerPaymentResponse;
import com.backend.water_management_system.service.CustomerPaymentService;

@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequestMapping("/api/customer/payments")
public class CustomerPaymentController {
    
    private final CustomerPaymentService customerPaymentService;

    public CustomerPaymentController(CustomerPaymentService customerPaymentService) {
        this.customerPaymentService = customerPaymentService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<CustomerPaymentResponse> initiateCustomerPayment(@RequestBody AddPaymentRequest request) {
        CustomerPaymentResponse response = customerPaymentService.initiateCustomerPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notify")
    public ResponseEntity<String> handlePayhereNotify(@RequestParam Map<String, String> params) {
        customerPaymentService.handlePayhereNotification(params);
        return ResponseEntity.ok("Notification received");
    }
}
