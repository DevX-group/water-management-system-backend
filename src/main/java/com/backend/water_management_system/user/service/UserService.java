package com.backend.water_management_system.user.service;

import com.backend.water_management_system.auth.service.AuthService;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.service.ActivityAuditService;
import com.backend.water_management_system.user.dto.UserCreateRequest;
import com.backend.water_management_system.user.dto.UserResponse;
import com.backend.water_management_system.user.dto.UserUpdateRequest;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final ActivityAuditService activityAuditService;

    @Transactional
    public UserResponse createUser(UserCreateRequest request, Role requesterRole) {
        return createUser(request, requesterRole, false);
    }

    @Transactional
    public UserResponse createPublicCustomerUser(UserCreateRequest request, Role requesterRole) {
        if (request.role() != Role.CUSTOMER) {
            throw new IllegalArgumentException("Public registration may only create customer accounts.");
        }
        return createUser(request, requesterRole, true);
    }

    private UserResponse createUser(UserCreateRequest request, Role requesterRole, boolean publicRegistration) {
        // Enforce RBAC: SYSTEM_ADMIN can only create CUSTOMERs
        if (requesterRole == Role.SYSTEM_ADMIN && request.role() != Role.CUSTOMER) {
            throw new IllegalArgumentException("System Admins can only create Customer accounts.");
        }

        if (request.role() != Role.CUSTOMER && (request.fullName() == null || request.fullName().isBlank())) {
            throw new IllegalArgumentException("Name is required for account creation");
        }
        
        if (userRepository.existsByNic(request.nic())) {
            throw new IllegalArgumentException("User with this NIC already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with this Email already exists");
        }

        User user = User.builder()
                .nic(request.nic())
                .fullName(request.fullName())
                .email(request.email())
                .role(request.role())
                .phoneNumber(request.phoneNumber())
                .status(UserStatus.PENDING_ACTIVATION) // Always start as pending
                .build();

        User savedUser = userRepository.save(user);

        // Send the activation link (which creates the token and emails it)
        authService.sendActivationLink(savedUser);

        Map<String, String> changes = Map.of(
                "role", savedUser.getRole().name(),
                "status", savedUser.getStatus().name());
        if (publicRegistration) {
            activityAuditService.recordPublicWeb(
                    AuditAction.USER_CREATED, AuditEntityType.USER, savedUser.getId(), changes);
        } else {
            activityAuditService.recordAuthenticatedWeb(
                    AuditAction.USER_CREATED, AuditEntityType.USER, savedUser.getId(), changes);
        }

        log.info("Created new user {} and sent activation link", savedUser.getId());
        return UserResponse.fromEntity(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(Role targetRole, Role requesterRole) {
        // Enforce RBAC: SYSTEM_ADMIN can only view CUSTOMERs
        if (requesterRole == Role.SYSTEM_ADMIN) {
            if (targetRole != null && targetRole != Role.CUSTOMER) {
                throw new IllegalArgumentException("System Admins can only view Customer accounts.");
            }
            targetRole = Role.CUSTOMER; // Force filtering to CUSTOMER if no role specified
        }

        List<User> users = (targetRole != null) 
                ? userRepository.findAllByRole(targetRole) 
                : userRepository.findAll();

        return users.stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateUserStatus(UUID userId, UserStatus newStatus, Role requesterRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Enforce RBAC: SYSTEM_ADMIN can only modify CUSTOMERs
        if (requesterRole == Role.SYSTEM_ADMIN && user.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException("System Admins can only manage Customer accounts.");
        }

        UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);
        User savedUser = userRepository.save(user);

        if (oldStatus != newStatus) {
            activityAuditService.recordAuthenticatedWeb(
                    AuditAction.USER_STATUS_CHANGED,
                    AuditEntityType.USER,
                    savedUser.getId(),
                    Map.of("status", oldStatus.name() + " -> " + newStatus.name()));
        }

        log.info("Updated status for user {} to {}", user.getId(), newStatus);
        return UserResponse.fromEntity(savedUser);
    }

    @Transactional
    public UserResponse updateAdmin(UUID userId, UserUpdateRequest request, Role requesterRole) {
        if (requesterRole != Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Only Super Admins can update admin accounts.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() == Role.CUSTOMER) {
            throw new IllegalArgumentException("Customer accounts cannot be updated here.");
        }

        if (request.role() == Role.CUSTOMER) {
            throw new IllegalArgumentException("Admin accounts cannot be changed to CUSTOMER.");
        }

        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required for admin accounts.");
        }

        userRepository.findByNic(request.nic())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(userId)) {
                        throw new IllegalArgumentException("User with this NIC already exists");
                    }
                });

        userRepository.findByEmail(request.email())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(userId)) {
                        throw new IllegalArgumentException("User with this Email already exists");
                    }
                });

        String oldFullName = user.getFullName();
        String oldNic = user.getNic();
        String oldEmail = user.getEmail();
        String oldPhoneNumber = user.getPhoneNumber();
        Role oldRole = user.getRole();

        user.setFullName(request.fullName());
        user.setNic(request.nic());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setRole(request.role());

        User savedUser = userRepository.save(user);

        Map<String, String> profileChanges = new java.util.LinkedHashMap<>();
        if (!Objects.equals(oldFullName, request.fullName())) profileChanges.put("fullName", "changed");
        if (!Objects.equals(oldNic, request.nic())) profileChanges.put("nic", "changed");
        if (!Objects.equals(oldEmail, request.email())) profileChanges.put("email", "changed");
        if (!Objects.equals(oldPhoneNumber, request.phoneNumber())) profileChanges.put("phoneNumber", "changed");
        if (!profileChanges.isEmpty()) {
            activityAuditService.recordAuthenticatedWeb(
                    AuditAction.USER_PROFILE_UPDATED, AuditEntityType.USER, savedUser.getId(), profileChanges);
        }
        if (oldRole != request.role()) {
            activityAuditService.recordAuthenticatedWeb(
                    AuditAction.USER_ROLE_CHANGED,
                    AuditEntityType.USER,
                    savedUser.getId(),
                    Map.of("role", oldRole.name() + " -> " + request.role().name()));
        }
        log.info("Updated admin user {}", savedUser.getId());
        return UserResponse.fromEntity(savedUser);
    }
}
