package com.backend.water_management_system.customer.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.customer.dto.CustomerRegistrationRequest;
import com.backend.water_management_system.customer.dto.CustomerSearchResponse;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.service.CustomerService;
import com.backend.water_management_system.security.UserPrincipal;
import com.backend.water_management_system.user.enums.Role;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Customer> registerCustomer(
            @Valid @RequestBody CustomerRegistrationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Role requesterRole = principal.getUser().getRole();
        Customer createdCustomer = customerService.registerCustomer(request, requesterRole);
        return ResponseEntity.status(201).body(createdCustomer);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public List<CustomerSearchResponse> searchCustomers(
            @RequestParam String query) {
        return customerService.searchCustomers(query);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public List<Customer> getAllCustomers() 
    {
        return customerService.getAllCustomers();
    }

    @GetMapping("{subscriptionNumber}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public Customer getCustomerById(@PathVariable String subscriptionNumber)
    {
        return customerService.getCustomerById(subscriptionNumber);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{subscriptionNumber}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable String subscriptionNumber,
            @Valid @RequestBody com.backend.water_management_system.customer.dto.CustomerUpdateRequest request) {
        Customer updatedCustomer = customerService.updateCustomer(subscriptionNumber, request);
        return ResponseEntity.ok(updatedCustomer);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{subscriptionNumber}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String subscriptionNumber) {
        customerService.deleteCustomer(subscriptionNumber);
        return ResponseEntity.noContent().build();
    }
}

