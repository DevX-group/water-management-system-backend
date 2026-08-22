package com.backend.water_management_system.customer.service;

import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.repository.CustomerRepository;
import com.backend.water_management_system.security.UserPrincipal;
import com.backend.water_management_system.user.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerAccessService {

    private final CustomerRepository customerRepository;

    public String getSubscriptionNumber(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null) {
            throw new AccessDeniedException("Unauthenticated request");
        }

        String nic = principal.getUser().getNic();
        Customer customer = customerRepository.findByUser_Nic(nic)
                .orElseThrow(() -> new AccessDeniedException("Customer account not found"));

        return customer.getSubscriptionNumber();
    }

    public String enforceOwnership(UserPrincipal principal, String requestedSubscription) {
        if (principal == null || principal.getUser() == null) {
            throw new AccessDeniedException("Unauthenticated request");
        }

        if ("me".equalsIgnoreCase(requestedSubscription)) {
            return getSubscriptionNumber(principal);
        }

        Role role = principal.getUser().getRole();
        if (role != Role.CUSTOMER) {
            return requestedSubscription;
        }

        String ownedSubscription = getSubscriptionNumber(principal);
        if (!ownedSubscription.equals(requestedSubscription)) {
            throw new AccessDeniedException("Forbidden");
        }

        return ownedSubscription;
    }
}
