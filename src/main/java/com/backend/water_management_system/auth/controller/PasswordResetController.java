package com.backend.water_management_system.auth.controller;

import com.backend.water_management_system.auth.dto.PasswordResetCompleteRequest;
import com.backend.water_management_system.auth.dto.PasswordResetRequest;
import com.backend.water_management_system.auth.dto.PasswordResetVerifyRequest;
import com.backend.water_management_system.auth.exception.PasswordResetException;
import com.backend.water_management_system.auth.exception.PasswordResetRateLimitException;
import com.backend.water_management_system.auth.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<?> request(@Valid @RequestBody PasswordResetRequest request,
                                     HttpServletRequest httpRequest) {
        try {
            return ResponseEntity.ok(passwordResetService.request(request, httpRequest.getRemoteAddr()));
        } catch (PasswordResetRateLimitException exception) {
            return ResponseEntity.status(429)
                    .header(HttpHeaders.RETRY_AFTER, "900")
                    .body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody PasswordResetVerifyRequest request,
                                    HttpServletRequest httpRequest) {
        try {
            return ResponseEntity.ok(passwordResetService.verify(request, httpRequest.getRemoteAddr()));
        } catch (PasswordResetRateLimitException exception) {
            return ResponseEntity.status(429)
                    .header(HttpHeaders.RETRY_AFTER, "900")
                    .body(Map.of("message", exception.getMessage()));
        } catch (PasswordResetException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/complete")
    public ResponseEntity<?> complete(@Valid @RequestBody PasswordResetCompleteRequest request) {
        try {
            return ResponseEntity.ok(passwordResetService.complete(request));
        } catch (PasswordResetException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }
}
