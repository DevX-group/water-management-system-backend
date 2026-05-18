package com.backend.water_management_system.user.controller;

import com.backend.water_management_system.security.UserPrincipal;
import com.backend.water_management_system.user.dto.UserCreateRequest;
import com.backend.water_management_system.user.dto.UserResponse;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Only SUPER_ADMIN and SYSTEM_ADMIN can create users
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> createUser(
            @Valid @RequestBody UserCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            Role requesterRole = principal.getUser().getRole();
            UserResponse response = userService.createUser(request, requesterRole);
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Role is optional. If provided, filters by role.
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestParam(required = false) Role role,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        Role requesterRole = principal.getUser().getRole();
        List<UserResponse> users = userService.getUsers(role, requesterRole);
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable UUID id, 
            @RequestParam UserStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            Role requesterRole = principal.getUser().getRole();
            UserResponse response = userService.updateUserStatus(id, status, requesterRole);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
