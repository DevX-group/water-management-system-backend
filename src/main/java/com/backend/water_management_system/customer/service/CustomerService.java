package com.backend.water_management_system.customer.service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.service.ActivityAuditService;
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
    private final ActivityAuditService activityAuditService;

    public CustomerService(CustomerRepository customerRepository, UserService userService, UserRepository userRepository, RegionRepository regionRepository, ActivationTokenRepository activationTokenRepository, ActivityAuditService activityAuditService) {
        this.customerRepository = customerRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.activityAuditService = activityAuditService;
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
        return customerRepository.findByUser_Nic(nic)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    @Transactional
    public Customer updateCustomerProfileByNic(String nic, com.backend.water_management_system.customer.dto.CustomerProfileUpdateRequest request) {
        Customer customer = getCustomerByNic(nic);
        String oldAccountHolderName = customer.getAccountHolderName();
        customer.setAccountHolderName(request.accountHolderName());
        
        User user = customer.getUser();
        Map<String, String> changes = new LinkedHashMap<>();
        if (user != null) {
            if (!Objects.equals(user.getFullName(), request.accountHolderName())) changes.put("fullName", "changed");
            if (!Objects.equals(user.getEmail(), request.email())) changes.put("email", "changed");
            if (!Objects.equals(user.getPhoneNumber(), request.phoneNumber())) changes.put("phoneNumber", "changed");
            user.setFullName(request.accountHolderName());
            user.setEmail(request.email());
            user.setPhoneNumber(request.phoneNumber());
            userRepository.save(user);
        }
        Customer savedCustomer = customerRepository.save(customer);
        if (user != null && (!changes.isEmpty()
                || !Objects.equals(oldAccountHolderName, request.accountHolderName()))) {
            if (!Objects.equals(oldAccountHolderName, request.accountHolderName())) changes.put("fullName", "changed");
            activityAuditService.recordAuthenticatedWeb(
                    AuditAction.USER_PROFILE_UPDATED, AuditEntityType.USER, user.getId(), changes);
        }
        return savedCustomer;
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
        UserResponse userRes = userService.createPublicCustomerUser(userReq, requesterRole);
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

        Map<String, String> changes = new LinkedHashMap<>();
        if (!Objects.equals(customer.getAccountHolderName(), request.accountHolderName())) changes.put("fullName", "changed");
        customer.setAccountHolderName(request.accountHolderName());
        customer.setAddress(request.address());
        customer.setConnectionType(request.connectionType());
        customer.setRegion(region);

        User user = customer.getUser();
        if (!Objects.equals(user.getNic(), request.nic())) changes.put("nic", "changed");
        if (!Objects.equals(user.getPhoneNumber(), request.phoneNumber())) changes.put("phoneNumber", "changed");
        if (request.email() != null && !request.email().isEmpty()
                && !Objects.equals(user.getEmail(), request.email())) changes.put("email", "changed");
        user.setNic(request.nic());
        user.setPhoneNumber(request.phoneNumber());
        if (request.email() != null && !request.email().isEmpty()) {
            user.setEmail(request.email());
        }

        Customer savedCustomer = customerRepository.save(customer);
        if (!changes.isEmpty()) {
            activityAuditService.recordAuthenticatedWeb(
                    AuditAction.USER_PROFILE_UPDATED, AuditEntityType.USER, user.getId(), changes);
        }
        return savedCustomer;
    }

    @Transactional
    public void deleteCustomer(String subscriptionNumber) {
        Customer customer = customerRepository.findById(subscriptionNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        User user = customer.getUser();
        UserStatus oldStatus = user.getStatus();
        activationTokenRepository.deleteAllByUser(user);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        if (oldStatus != UserStatus.INACTIVE) {
            activityAuditService.recordAuthenticatedWeb(
                    AuditAction.USER_STATUS_CHANGED,
                    AuditEntityType.USER,
                    user.getId(),
                    Map.of("status", oldStatus.name() + " -> " + UserStatus.INACTIVE.name()));
        }
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
