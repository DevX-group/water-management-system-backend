package com.backend.water_management_system.customer.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.customer.dto.CustomerSearchResponse;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.repository.CustomerRepository;

import com.backend.water_management_system.customer.dto.CustomerRegistrationRequest;
import com.backend.water_management_system.user.dto.UserCreateRequest;
import com.backend.water_management_system.user.dto.UserResponse;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.UserRepository;
import com.backend.water_management_system.user.repository.ActivationTokenRepository;
import com.backend.water_management_system.user.service.UserService;
import com.backend.water_management_system.common.entity.Region;
import com.backend.water_management_system.common.repository.RegionRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final ActivationTokenRepository activationTokenRepository;

    public CustomerService(CustomerRepository customerRepository, UserService userService, UserRepository userRepository, RegionRepository regionRepository, ActivationTokenRepository activationTokenRepository) {
        this.customerRepository = customerRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
        this.activationTokenRepository = activationTokenRepository;
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

    public Customer getCustomerByNic(String nic) {
        return customerRepository.findByUser_Nic(nic).orElseThrow(() -> new RuntimeException("Customer not found for NIC " + nic));
    }

    @Transactional
    public Customer updateCustomerProfileByNic(String nic, com.backend.water_management_system.customer.dto.CustomerProfileUpdateRequest request) {
        Customer customer = getCustomerByNic(nic);
        customer.setAccountHolderName(request.accountHolderName());
        
        User user = customer.getUser();
        if (user != null) {
            user.setFullName(request.accountHolderName());
            user.setEmail(request.email());
            user.setPhoneNumber(request.phoneNumber());
            userRepository.save(user);
        }
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer registerCustomer(CustomerRegistrationRequest request, Role requesterRole) {
        // 1. Fetch Region
        Region region = regionRepository.findById(request.regionCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid region code"));

        // 2. Create Auth User (this also triggers the activation email)
        UserCreateRequest userReq = new UserCreateRequest(
                request.nic(),
                request.accountHolderName(),
                request.email(),
                Role.CUSTOMER,
                request.phoneNumber()
        );
        UserResponse userRes = userService.createUser(userReq, requesterRole);
        User user = userRepository.findById(userRes.id())
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve created user"));

        // 3. Create Customer
        String tempSubNumber = generateSubscriptionNumber(region.getRegionCode());
        
        Customer customer = new Customer(
                tempSubNumber,
                request.accountHolderName(),
                user,
                request.address(),
                request.connectionType(),
                region
        );
        customer.setOutstandingBalance(java.math.BigDecimal.ZERO);

        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(String subscriptionNumber, com.backend.water_management_system.customer.dto.CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Region region = regionRepository.findById(request.regionCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid region code"));

        customer.setAccountHolderName(request.accountHolderName());
        customer.setAddress(request.address());
        customer.setConnectionType(request.connectionType());
        customer.setRegion(region);

        User user = customer.getUser();
        user.setNic(request.nic());
        user.setPhoneNumber(request.phoneNumber());
        if (request.email() != null && !request.email().isEmpty()) {
            user.setEmail(request.email());
        }

        return customerRepository.save(customer);
    }

    @Transactional
    public void deleteCustomer(String subscriptionNumber) {
        Customer customer = customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        User user = customer.getUser();
        activationTokenRepository.deleteAllByUser(user);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    private String generateSubscriptionNumber(String regionCode) {
        String subscriptionNumber;
        int maxRetries = 10;
        int attempts = 0;

        do {
            // Generate a random 8-digit number (10000000 to 99999999)
            int randomNum = ThreadLocalRandom.current().nextInt(10000000, 100000000);
            subscriptionNumber = String.format("%s-%08d", regionCode, randomNum);
            attempts++;
            
            if (attempts > maxRetries) {
                throw new IllegalStateException("Failed to generate unique subscription number after " + maxRetries + " attempts");
            }
        } while (customerRepository.existsById(subscriptionNumber));

        return subscriptionNumber;
    }
}
