package com.backend.water_management_system.auth.dto;

// Returned to the client after a successful login
public record LoginResponse(
        String token,
        String role,
        String nic,
        String email
) {}
