package com.backend.water_management_system.customer.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.customer.dto.CustomerSearchResponse;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.repository.CustomerRepository;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<CustomerSearchResponse> searchCustomers(String query) {
        List<Customer> customers = customerRepository.searchCustomers(query);
        return customers.stream()
                .map(c -> new CustomerSearchResponse(c.getSubscriptionNumber(), c.getAccountHolderName()))
                .toList();
    }

    public List<Customer> getAllCustomers() 
    {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(String subscriptionNumber)
    {
        return customerRepository.findById(subscriptionNumber).orElseThrow(() -> new RuntimeException("Customer not found by subscription number " + subscriptionNumber));
    }
}
