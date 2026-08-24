package com.backend.water_management_system.auth.service;

import com.backend.water_management_system.auth.dto.ActivationRequest;
import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.service.ActivityAuditService;
import com.backend.water_management_system.auth.dto.LoginRequest;
import com.backend.water_management_system.auth.dto.LoginResponse;
import com.backend.water_management_system.security.JwtService;
import com.backend.water_management_system.security.UserPrincipal;
import com.backend.water_management_system.user.entity.ActivationToken;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.ActivationTokenRepository;
import com.backend.water_management_system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivationMessageService activationMessageService;
    private final ActivityAuditService activityAuditService;

    @Value("${app.activation-token-expiry-hours:72}")
    private int activationTokenExpiryHours;

    public LoginResponse login(LoginRequest request) {
        // AuthenticationManager handles credential validation + account status checks
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.nic(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        String token = jwtService.generateToken(user.getNic(), user.getRole().name(), user.getTokenVersion());

        return new LoginResponse(token, user.getRole().name(), user.getNic(), user.getEmail());
    }

    // ── Account activation ────────────────────────────────────────────────────

    // Generates an activation token for the given user and sends it via email.
    // Any previously unused token for this user is invalidated first.
    @Transactional
    public void sendActivationLink(User user) {
        // Invalidate any existing unused token for this user
        activationTokenRepository.findByUserAndUsedFalse(user)
                .ifPresent(existing -> {
                    existing.setUsed(true);
                    activationTokenRepository.save(existing);
                });

        // Create a new token
        String tokenValue = UUID.randomUUID().toString();
        ActivationToken activationToken = ActivationToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(activationTokenExpiryHours))
                .used(false)
                .build();
        activationTokenRepository.save(activationToken);

        // Send activation message
        activationMessageService.sendActivationEmail(user.getEmail(), tokenValue);
    }

    // Validates the activation token, sets the user's password, and marks the account as active.
    @Transactional
    public void activateAccount(ActivationRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        ActivationToken activationToken = activationTokenRepository
                .findByToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Invalid activation link"));

        if (activationToken.isUsed()) {
            throw new IllegalStateException("This activation link has already been used");
        }

        if (activationToken.isExpired()) {
            throw new IllegalStateException("This activation link has expired. Please request a new one");
        }

        User user = activationToken.getUser();
        if (user.getStatus() == UserStatus.INACTIVE || user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("This account is deactivated. Please contact support.");
        }
        UserStatus oldStatus = user.getStatus();
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        activationToken.setUsed(true);
        activationTokenRepository.save(activationToken);

        if (oldStatus != UserStatus.ACTIVE) {
            activityAuditService.recordPublicWeb(
                    AuditAction.USER_ACTIVATED,
                    AuditEntityType.USER,
                    user.getId(),
                    Map.of("status", oldStatus.name() + " -> " + UserStatus.ACTIVE.name()));
        }
    }
}
