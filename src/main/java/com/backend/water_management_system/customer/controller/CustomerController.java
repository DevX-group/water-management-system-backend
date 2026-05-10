package com.backend.water_management_system.customer.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.customer.dto.CustomerSearchResponse;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.service.CustomerService;

@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/search")
    public List<CustomerSearchResponse> searchCustomers(
            @RequestParam String query) {
        return customerService.searchCustomers(query);
    }

    @GetMapping
    public List<Customer> getAllCustomers() 
    {
        return customerService.getAllCustomers();
    }

    @GetMapping("{subscriptionNumber}")
    public Customer getCustomerById(@PathVariable String subscriptionNumber)
    {
        return customerService.getCustomerById(subscriptionNumber);
    }
}
