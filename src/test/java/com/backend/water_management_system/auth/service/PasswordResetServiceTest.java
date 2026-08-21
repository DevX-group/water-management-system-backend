package com.backend.water_management_system.auth.service;

import com.backend.water_management_system.auth.config.PasswordResetProperties;
import com.backend.water_management_system.auth.delivery.PasswordResetDeliveryContext;
import com.backend.water_management_system.auth.delivery.PasswordResetDeliveryService;
import com.backend.water_management_system.auth.dto.PasswordResetCompleteRequest;
import com.backend.water_management_system.auth.dto.PasswordResetRequest;
import com.backend.water_management_system.auth.dto.PasswordResetVerifyRequest;
import com.backend.water_management_system.auth.entity.DeliveryChannel;
import com.backend.water_management_system.auth.entity.PasswordResetAuthorization;
import com.backend.water_management_system.auth.entity.PasswordResetChallenge;
import com.backend.water_management_system.auth.entity.PasswordResetPurpose;
import com.backend.water_management_system.auth.exception.PasswordResetException;
import com.backend.water_management_system.auth.rate.PasswordResetRateLimiter;
import com.backend.water_management_system.auth.security.PasswordResetCryptography;
import com.backend.water_management_system.auth.repository.PasswordResetAuthorizationRepository;
import com.backend.water_management_system.auth.repository.PasswordResetChallengeRepository;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetChallengeRepository challengeRepository;
    @Mock PasswordResetAuthorizationRepository authorizationRepository;
    @Mock PasswordResetRateLimiter rateLimiter;
    @Mock PasswordResetDeliveryService deliveryService;
    @Mock PasswordEncoder passwordEncoder;

    private PasswordResetProperties properties;
    private PasswordResetCryptography cryptography;
    private PasswordResetService service;
    private User activeUser;
    private Executor executor;

    @BeforeEach
    void setUp() {
        properties = new PasswordResetProperties();
        properties.setOtpHmacSecret("test-only-password-reset-secret");
        cryptography = new PasswordResetCryptography(properties);
        executor = Runnable::run;
        service = new PasswordResetService(
                userRepository,
                challengeRepository,
                authorizationRepository,
                properties,
                cryptography,
                rateLimiter,
                List.of(deliveryService),
                passwordEncoder,
                executor);
        activeUser = User.builder()
                .id(UUID.randomUUID())
                .nic("200012345678")
                .email("reset@example.com")
                .fullName("Test User")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
        lenient().when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        lenient().when(deliveryService.supports(DeliveryChannel.EMAIL)).thenReturn(true);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void eligibleRequestPersistsOnlyHmacAndDeliversSixDigitOtp() {
        when(userRepository.findLockedByNic(activeUser.getNic())).thenReturn(Optional.of(activeUser));
        when(challengeRepository.findFirstByUserAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                any(), any(), any())).thenReturn(Optional.empty());

        Map<String, String> response = service.request(new PasswordResetRequest(activeUser.getNic()), "127.0.0.1");

        assertThat(response).containsEntry("message", PasswordResetService.REQUEST_MESSAGE);
        ArgumentCaptor<PasswordResetChallenge> challengeCaptor = ArgumentCaptor.forClass(PasswordResetChallenge.class);
        verify(challengeRepository).save(challengeCaptor.capture());
        assertThat(challengeCaptor.getValue().getOtpHmac()).hasSize(64);
        ArgumentCaptor<PasswordResetDeliveryContext> contextCaptor = ArgumentCaptor.forClass(PasswordResetDeliveryContext.class);
        verify(deliveryService).deliver(contextCaptor.capture());
        assertThat(contextCaptor.getValue().getRawOtp()).matches("\\d{6}");
    }

    @Test
    void nonexistentAndIneligibleAccountsReturnSameGenericResponseWithoutChallengeOrEmail() {
        when(userRepository.findLockedByNic(anyString())).thenReturn(Optional.empty());
        Map<String, String> nonexistent = service.request(new PasswordResetRequest("200012345678"), "127.0.0.1");

        User inactive = User.builder().id(UUID.randomUUID()).nic("200012345679").status(UserStatus.INACTIVE).build();
        when(userRepository.findLockedByNic(inactive.getNic())).thenReturn(Optional.of(inactive));
        Map<String, String> ineligible = service.request(new PasswordResetRequest(inactive.getNic()), "127.0.0.1");

        assertThat(nonexistent).isEqualTo(ineligible);
        verify(challengeRepository, never()).save(any());
        verify(deliveryService, never()).deliver(any());
    }

    @Test
    void resendCooldownReturnsGenericResponseAndDoesNotReplaceChallenge() {
        when(userRepository.findLockedByNic(activeUser.getNic())).thenReturn(Optional.of(activeUser));
        PasswordResetChallenge active = PasswordResetChallenge.builder()
                .user(activeUser)
                .purpose(PasswordResetPurpose.PASSWORD_RESET)
                .resendAvailableAt(Instant.now().plusSeconds(30))
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(challengeRepository.findFirstByUserAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                any(), any(), any())).thenReturn(Optional.of(active));

        Map<String, String> response = service.request(new PasswordResetRequest(activeUser.getNic()), "127.0.0.1");

        assertThat(response).containsEntry("message", PasswordResetService.REQUEST_MESSAGE);
        verify(challengeRepository, never()).invalidateActiveByUserAndPurpose(any(), any(), any());
        verify(deliveryService, never()).deliver(any());
    }

    @Test
    void invalidOtpIncrementsAttemptsAndReturnsGenericError() {
        when(userRepository.findByNic(activeUser.getNic())).thenReturn(Optional.of(activeUser));
        PasswordResetChallenge challenge = PasswordResetChallenge.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .purpose(PasswordResetPurpose.PASSWORD_RESET)
                .otpHmac("expected-hmac")
                .failedAttempts(0)
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(challengeRepository.findFirstByUserAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                any(), any(), any())).thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> service.verify(new PasswordResetVerifyRequest(activeUser.getNic(), "000000"), "127.0.0.1"))
                .isInstanceOf(PasswordResetException.class)
                .hasMessage(PasswordResetService.INVALID_CODE_MESSAGE);
        assertThat(challenge.getFailedAttempts()).isEqualTo(1);
        verify(challengeRepository).save(challenge);
        verify(authorizationRepository, never()).save(any());
    }

    @Test
    void exhaustedChallengeDoesNotPerformAdditionalOtpCheck() {
        when(userRepository.findByNic(activeUser.getNic())).thenReturn(Optional.of(activeUser));
        PasswordResetChallenge challenge = PasswordResetChallenge.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .purpose(PasswordResetPurpose.PASSWORD_RESET)
                .otpHmac("expected-hmac")
                .failedAttempts(properties.getMaxOtpAttempts())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(challengeRepository.findFirstByUserAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                any(), any(), any())).thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> service.verify(new PasswordResetVerifyRequest(activeUser.getNic(), "000000"), "127.0.0.1"))
                .isInstanceOf(PasswordResetException.class)
                .hasMessage(PasswordResetService.INVALID_CODE_MESSAGE);
        verify(challengeRepository, never()).save(any());
    }

    @Test
    void validOtpIsConsumedAndReturnsOpaqueAuthorizationOnce() {
        when(userRepository.findByNic(activeUser.getNic())).thenReturn(Optional.of(activeUser));
        PasswordResetChallenge challenge = PasswordResetChallenge.builder()
                .id(UUID.randomUUID()).user(activeUser).purpose(PasswordResetPurpose.PASSWORD_RESET)
                .failedAttempts(0).expiresAt(Instant.now().plusSeconds(300)).build();
        challenge.setOtpHmac(cryptography.otpHmac(challenge.getId().toString(), activeUser.getId().toString(),
                PasswordResetPurpose.PASSWORD_RESET.name(), "123456"));
        when(challengeRepository.findFirstByUserAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                any(), any(), any())).thenReturn(Optional.of(challenge));

        Map<String, String> response = service.verify(
                new PasswordResetVerifyRequest(activeUser.getNic(), "123456"), "127.0.0.1");

        assertThat(response.get("resetAuthorization")).isNotBlank().hasSizeGreaterThanOrEqualTo(43);
        assertThat(challenge.getUsedAt()).isNotNull();
        verify(authorizationRepository).save(any(PasswordResetAuthorization.class));
    }

    @Test
    void invalidAndExpiredAuthorizationsAreRejectedWithoutChangingUser() {
        assertThatThrownBy(() -> service.complete(new PasswordResetCompleteRequest(
                "missing", "new-password", "new-password")))
                .isInstanceOf(PasswordResetException.class)
                .hasMessage("Reset authorization is invalid or has expired.");

        PasswordResetAuthorization expired = PasswordResetAuthorization.builder()
                .user(activeUser)
                .authorizationDigest(cryptography.authorizationDigest("expired"))
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        when(authorizationRepository.findByAuthorizationDigest(expired.getAuthorizationDigest()))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.complete(new PasswordResetCompleteRequest(
                "expired", "new-password", "new-password")))
                .isInstanceOf(PasswordResetException.class)
                .hasMessage("Reset authorization is invalid or has expired.");
        verify(userRepository, never()).findLockedById(any());
    }

    @Test
    void emailFailureDoesNotChangeGenericRequestResponse() {
        when(userRepository.findLockedByNic(activeUser.getNic())).thenReturn(Optional.of(activeUser));
        when(challengeRepository.findFirstByUserAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                any(), any(), any())).thenReturn(Optional.empty());
        doThrow(new RuntimeException("mail unavailable")).when(deliveryService).deliver(any());

        Map<String, String> response = service.request(new PasswordResetRequest(activeUser.getNic()), "127.0.0.1");

        assertThat(response).containsEntry("message", PasswordResetService.REQUEST_MESSAGE);
    }

    @Test
    void authorizationCompletionUpdatesPasswordAndTokenVersionOnce() {
        PasswordResetAuthorization authorization = PasswordResetAuthorization.builder()
                .user(activeUser)
                .authorizationDigest(cryptography.authorizationDigest("authorization"))
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        when(authorizationRepository.findByAuthorizationDigest(authorization.getAuthorizationDigest()))
                .thenReturn(Optional.of(authorization));
        when(userRepository.findLockedById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.encode("new-password")).thenReturn("bcrypt-hash");

        Map<String, String> response = service.complete(new PasswordResetCompleteRequest(
                "authorization", "new-password", "new-password"));

        assertThat(response).containsEntry("message", "Password reset successfully. You can now log in.");
        assertThat(activeUser.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(activeUser.getTokenVersion()).isEqualTo(1L);
        assertThat(authorization.getUsedAt()).isNotNull();
        verify(userRepository).save(activeUser);
        verify(authorizationRepository).save(authorization);
    }

    @Test
    void deliveryIsSubmittedOnlyAfterTransactionCommit() {
        when(userRepository.findLockedByNic(activeUser.getNic())).thenReturn(Optional.of(activeUser));
        when(challengeRepository.findFirstByUserAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                any(), any(), any())).thenReturn(Optional.empty());
        java.util.concurrent.atomic.AtomicInteger executions = new java.util.concurrent.atomic.AtomicInteger();
        Executor recordingExecutor = command -> executions.incrementAndGet();
        service = new PasswordResetService(userRepository, challengeRepository, authorizationRepository,
                properties, cryptography, rateLimiter, List.of(deliveryService), passwordEncoder, recordingExecutor);

        TransactionSynchronizationManager.initSynchronization();
        service.request(new PasswordResetRequest(activeUser.getNic()), "127.0.0.1");

        assertThat(executions).hasValue(0);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(executions).hasValue(1);
    }
}
