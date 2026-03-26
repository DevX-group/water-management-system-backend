package com.backend.water_management_system.service;

import com.backend.water_management_system.repository.CustomerRepository;

import java.util.List;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.dto.CustomerSearchResponse;
import com.backend.water_management_system.entity.Customer;

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
}
