package com.backend.water_management_system.user.service;

import com.backend.water_management_system.auth.service.AuthService;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    @Transactional
    public UserResponse createUser(UserCreateRequest request, Role requesterRole) {
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

        log.info("Created new user with NIC: {} and sent activation link", savedUser.getNic());
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

        user.setStatus(newStatus);
        User savedUser = userRepository.save(user);

        log.info("Updated status for user {} to {}", user.getNic(), newStatus);
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

        user.setFullName(request.fullName());
        user.setNic(request.nic());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setRole(request.role());

        User savedUser = userRepository.save(user);
        log.info("Updated admin user {}", savedUser.getNic());
        return UserResponse.fromEntity(savedUser);
    }
}
