package com.backend.water_management_system.auth.service;

import com.backend.water_management_system.activity_audit.enums.AuditAction;
import com.backend.water_management_system.activity_audit.enums.AuditEntityType;
import com.backend.water_management_system.activity_audit.service.ActivityAuditService;
import com.backend.water_management_system.auth.dto.ActivationRequest;
import com.backend.water_management_system.security.JwtService;
import com.backend.water_management_system.user.entity.ActivationToken;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.ActivationTokenRepository;
import com.backend.water_management_system.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceAuditTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock UserRepository userRepository;
    @Mock ActivationTokenRepository activationTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ActivationMessageService activationMessageService;
    @Mock ActivityAuditService auditService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(authenticationManager, jwtService, userRepository,
                activationTokenRepository, passwordEncoder, activationMessageService, auditService);
    }

    @Test
    void activationRecordsOnePublicSafeTransitionWithoutToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).role(Role.CUSTOMER)
                .status(UserStatus.PENDING_ACTIVATION).build();
        ActivationToken token = ActivationToken.builder().token("secret-token").user(user)
                .expiresAt(LocalDateTime.now().plusHours(1)).build();
        when(activationTokenRepository.findByToken("secret-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("password123")).thenReturn("secret-hash");

        service.activateAccount(new ActivationRequest("secret-token", "password123", "password123"));

        verify(auditService, times(1)).recordPublicWeb(AuditAction.USER_ACTIVATED,
                AuditEntityType.USER, userId,
                Map.of("status", "PENDING_ACTIVATION -> ACTIVE"));
        verify(auditService, never()).recordAuthenticatedWeb(any(), any(), any(), any());
    }

    @Test
    void invalidActivationCreatesNoAuditEntry() {
        when(activationTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateAccount(
                new ActivationRequest("invalid", "password123", "password123")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(auditService);
    }
}
