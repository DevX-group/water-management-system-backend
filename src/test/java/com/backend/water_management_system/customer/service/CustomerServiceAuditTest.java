package com.backend.water_management_system.customer.service;

import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.service.ActivityAuditService;
import com.backend.water_management_system.common.entity.Region;
import com.backend.water_management_system.common.repository.RegionRepository;
import com.backend.water_management_system.customer.dto.CustomerProfileUpdateRequest;
import com.backend.water_management_system.customer.dto.CustomerRegistrationRequest;
import com.backend.water_management_system.customer.dto.CustomerUpdateRequest;
import com.backend.water_management_system.customer.entity.Customer;
import com.backend.water_management_system.customer.repository.CustomerRepository;
import com.backend.water_management_system.user.dto.UserResponse;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.ActivationTokenRepository;
import com.backend.water_management_system.user.repository.UserRepository;
import com.backend.water_management_system.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceAuditTest {

    @Mock CustomerRepository customerRepository;
    @Mock UserService userService;
    @Mock UserRepository userRepository;
    @Mock RegionRepository regionRepository;
    @Mock ActivationTokenRepository activationTokenRepository;
    @Mock ActivityAuditService auditService;
    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(customerRepository, userService, userRepository,
                regionRepository, activationTokenRepository, auditService);
        lenient().when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void registrationDelegatesToSingleTrustedPublicUserCreationPath() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, UserStatus.PENDING_ACTIVATION);
        when(userService.createPublicCustomerUser(any(), eq(Role.SYSTEM_ADMIN)))
                .thenReturn(UserResponse.fromEntity(user));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(regionRepository.findById("R001")).thenReturn(Optional.of(
                Region.builder().regionCode("R001").regionName("Region").isActive(true).build()));

        service.registerCustomer(new CustomerRegistrationRequest("Name", "nic", "Address",
                "0712345678", "mail@example.test", "METERED", "R001"), Role.SYSTEM_ADMIN);

        verify(userService, times(1)).createPublicCustomerUser(any(), eq(Role.SYSTEM_ADMIN));
        verifyNoInteractions(auditService);
    }

    @Test
    void selfServiceProfileRecordsOnlyChangedMarkersAndNoOpIsSilent() {
        User user = user(UUID.randomUUID(), UserStatus.ACTIVE);
        Customer customer = customer(user);
        when(customerRepository.findByUser_Nic("lookup")).thenReturn(Optional.of(customer));

        service.updateCustomerProfileByNic("lookup",
                new CustomerProfileUpdateRequest("Changed", user.getEmail(), user.getPhoneNumber()));
        verify(auditService).recordAuthenticatedWeb(AuditAction.USER_PROFILE_UPDATED,
                AuditEntityType.USER, user.getId(), Map.of("fullName", "changed"));

        clearInvocations(auditService);
        service.updateCustomerProfileByNic("lookup",
                new CustomerProfileUpdateRequest(user.getFullName(), user.getEmail(), user.getPhoneNumber()));
        verifyNoInteractions(auditService);
    }

    @Test
    void administrativeCustomerUpdateRecordsOnlyApplicableChangedMarkers() {
        User user = user(UUID.randomUUID(), UserStatus.ACTIVE);
        Customer customer = customer(user);
        when(customerRepository.findById("SUB-1")).thenReturn(Optional.of(customer));
        when(regionRepository.findById("R002")).thenReturn(Optional.of(
                Region.builder().regionCode("R002").regionName("Other").isActive(true).build()));

        service.updateCustomer("SUB-1", new CustomerUpdateRequest(
                user.getFullName(), "changed-nic", "New Address", user.getPhoneNumber(),
                user.getEmail(), "NON_METERED", "R002"));

        verify(auditService).recordAuthenticatedWeb(AuditAction.USER_PROFILE_UPDATED,
                AuditEntityType.USER, user.getId(), Map.of("nic", "changed"));
    }

    @Test
    void deactivationRecordsStatusChangeNotDeletionAndNoOpIsSilent() {
        User user = user(UUID.randomUUID(), UserStatus.ACTIVE);
        when(customerRepository.findById("SUB-1")).thenReturn(Optional.of(customer(user)));

        service.deleteCustomer("SUB-1");

        verify(auditService).recordAuthenticatedWeb(AuditAction.USER_STATUS_CHANGED,
                AuditEntityType.USER, user.getId(), Map.of("status", "ACTIVE -> INACTIVE"));
        verify(auditService, times(1)).recordAuthenticatedWeb(any(), any(), any(), any());

        clearInvocations(auditService);
        service.deleteCustomer("SUB-1");
        verifyNoInteractions(auditService);
    }

    private static User user(UUID id, UserStatus status) {
        return User.builder().id(id).nic("nic").fullName("Name").email("mail@example.test")
                .phoneNumber("0712345678").role(Role.CUSTOMER).status(status).build();
    }

    private static Customer customer(User user) {
        return Customer.builder().subscriptionNumber("SUB-1").accountHolderName(user.getFullName())
                .user(user).address("Address").connectionType("METERED").build();
    }
}
