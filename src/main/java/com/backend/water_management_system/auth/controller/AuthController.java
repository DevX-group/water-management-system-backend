package com.backend.water_management_system.auth.controller;

import com.backend.water_management_system.auth.dto.ActivationRequest;
import com.backend.water_management_system.auth.dto.LoginRequest;
import com.backend.water_management_system.auth.dto.LoginResponse;
import com.backend.water_management_system.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (DisabledException e) {
            // Account exists but is PENDING_ACTIVATION or INACTIVE
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Your account is not activated yet. Please check your email for the activation link."));
        } catch (LockedException e) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Your account has been suspended. Please contact support."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Invalid NIC or password."));
        }
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activateAccount(@Valid @RequestBody ActivationRequest request) {
        try {
            authService.activateAccount(request);
            return ResponseEntity.ok(Map.of("message", "Account activated successfully. You can now log in."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(410)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
