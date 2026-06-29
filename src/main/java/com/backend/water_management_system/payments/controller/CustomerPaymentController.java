package com.backend.water_management_system.payments.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.backend.water_management_system.billing.dto.CurrentBillResponse;
import com.backend.water_management_system.billing.dto.OutstandingBillsSummaryResponse;
import com.backend.water_management_system.common.dto.PaginationResponse;
import com.backend.water_management_system.customer.service.CustomerAccessService;
import com.backend.water_management_system.payments.dto.CustomerAddPaymentRequest;
import com.backend.water_management_system.payments.dto.CustomerPaymentResponse;
import com.backend.water_management_system.payments.dto.PaymentHistoryItemResponse;
import com.backend.water_management_system.payments.enums.PaymentMethod;
import com.backend.water_management_system.payments.service.CustomerPaymentService;
import com.backend.water_management_system.security.UserPrincipal;

@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequestMapping("/api/customer/payments")
public class CustomerPaymentController {

    private final CustomerPaymentService customerPaymentService;
    private final CustomerAccessService customerAccessService;

    public CustomerPaymentController(CustomerPaymentService customerPaymentService,
                                     CustomerAccessService customerAccessService) {
        this.customerPaymentService = customerPaymentService;
        this.customerAccessService = customerAccessService;
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerPaymentResponse> initiateCustomerPayment(
            @Valid @RequestBody CustomerAddPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String subscriptionNumber = customerAccessService.getSubscriptionNumber(principal);
        CustomerPaymentResponse response = customerPaymentService.initiateCustomerPayment(request, subscriptionNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notify")
    public ResponseEntity<String> handlePayhereNotify(@RequestParam Map<String, String> params) {
        customerPaymentService.handlePayhereNotification(params);
        return ResponseEntity.ok("Notification received");
    }

    // Endpoint used by frontend success page to poll latest payment status
    @GetMapping("/status/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<String> getPaymentStatus(
            @PathVariable String orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        String subscriptionNumber = customerAccessService.getSubscriptionNumber(principal);
        String status = customerPaymentService.getPaymentStatus(orderId, subscriptionNumber);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaginationResponse<PaymentHistoryItemResponse>> getPaymentHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @AuthenticationPrincipal UserPrincipal principal) {

        String subscriptionNumber = customerAccessService.getSubscriptionNumber(principal);
        return ResponseEntity.ok(
                customerPaymentService.getPaymentHistoryForCustomer(page, size, year, paymentMethod, subscriptionNumber));
    }

    @GetMapping("/current-bill")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CurrentBillResponse> getCurrentBillForCustomer(
            @AuthenticationPrincipal UserPrincipal principal) {
        String subscriptionNumber = customerAccessService.getSubscriptionNumber(principal);
        CurrentBillResponse response = customerPaymentService.getCurrentBillForCustomer(subscriptionNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/outstanding-bills")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OutstandingBillsSummaryResponse> getOutstandingBillsForCustomer(
            @AuthenticationPrincipal UserPrincipal principal) {
        String subscriptionNumber = customerAccessService.getSubscriptionNumber(principal);
        OutstandingBillsSummaryResponse response = customerPaymentService.getOutstandingBillsForCustomer(subscriptionNumber);
        return ResponseEntity.ok(response);
    }

}