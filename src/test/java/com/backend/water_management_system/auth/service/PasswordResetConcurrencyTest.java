package com.backend.water_management_system.auth.service;

import com.backend.water_management_system.auth.config.PasswordResetProperties;
import com.backend.water_management_system.auth.delivery.PasswordResetDeliveryService;
import com.backend.water_management_system.auth.dto.PasswordResetCompleteRequest;
import com.backend.water_management_system.auth.dto.PasswordResetVerifyRequest;
import com.backend.water_management_system.auth.entity.PasswordResetAuthorization;
import com.backend.water_management_system.auth.entity.PasswordResetChallenge;
import com.backend.water_management_system.auth.entity.PasswordResetPurpose;
import com.backend.water_management_system.auth.rate.PasswordResetRateLimiter;
import com.backend.water_management_system.auth.repository.PasswordResetAuthorizationRepository;
import com.backend.water_management_system.auth.repository.PasswordResetChallengeRepository;
import com.backend.water_management_system.auth.security.PasswordResetCryptography;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetConcurrencyTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetChallengeRepository challengeRepository;
    @Mock PasswordResetAuthorizationRepository authorizationRepository;
    @Mock PasswordResetRateLimiter rateLimiter;
    @Mock PasswordResetDeliveryService deliveryService;
    @Mock PasswordEncoder passwordEncoder;

    private PasswordResetService service;
    private PasswordResetCryptography cryptography;
    private User user;
    private PasswordResetProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PasswordResetProperties();
        properties.setOtpHmacSecret("test-only-password-reset-secret");
        cryptography = new PasswordResetCryptography(properties);
        user = User.builder().id(UUID.randomUUID()).nic("200012345678")
                .email("concurrency@example.com").role(Role.CUSTOMER).status(UserStatus.ACTIVE).build();
        service = new PasswordResetService(userRepository, challengeRepository, authorizationRepository,
                properties, cryptography, rateLimiter, List.of(deliveryService), passwordEncoder, Runnable::run);
        lenient().when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        lenient().when(userRepository.findByNic(user.getNic())).thenReturn(Optional.of(user));
        lenient().when(userRepository.findLockedById(user.getId())).thenReturn(Optional.of(user));
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("bcrypt-hash");
    }

    @Test
    void simultaneousOtpVerificationHasOneWinner() throws Exception {
        PasswordResetChallenge challenge = PasswordResetChallenge.builder()
                .id(UUID.randomUUID()).user(user).purpose(PasswordResetPurpose.PASSWORD_RESET)
                .expiresAt(Instant.now().plusSeconds(300)).failedAttempts(0).build();
        challenge.setOtpHmac(cryptography.otpHmac(challenge.getId().toString(), user.getId().toString(),
                PasswordResetPurpose.PASSWORD_RESET.name(), "123456"));
        AtomicBoolean consumed = new AtomicBoolean();
        when(challengeRepository.findFirstByUserAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                any(), any(), any())).thenAnswer(invocation -> consumed.compareAndSet(false, true)
                ? Optional.of(challenge) : Optional.empty());

        AtomicInteger successes = runTogether(() -> service.verify(
                new PasswordResetVerifyRequest(user.getNic(), "123456"), "127.0.0.1"));

        assertThat(successes).hasValue(1);
    }

    @Test
    void simultaneousResetCompletionHasOneWinner() throws Exception {
        String rawAuthorization = "concurrent-authorization";
        PasswordResetAuthorization authorization = PasswordResetAuthorization.builder()
                .user(user).authorizationDigest(cryptography.authorizationDigest(rawAuthorization))
                .expiresAt(Instant.now().plusSeconds(600)).build();
        AtomicBoolean consumed = new AtomicBoolean();
        when(authorizationRepository.findByAuthorizationDigest(authorization.getAuthorizationDigest()))
                .thenAnswer(invocation -> consumed.compareAndSet(false, true)
                        ? Optional.of(authorization) : Optional.empty());

        AtomicInteger successes = runTogether(() -> service.complete(
                new PasswordResetCompleteRequest(rawAuthorization, "new-password", "new-password")));

        assertThat(successes).hasValue(1);
        assertThat(user.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(user.getTokenVersion()).isEqualTo(1L);
    }

    private AtomicInteger runTogether(CheckedOperation operation) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        for (int index = 0; index < 2; index++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    operation.run();
                    successes.incrementAndGet();
                } catch (Throwable throwable) {
                    failure.compareAndSet(null, throwable);
                }
            });
        }
        ready.await();
        start.countDown();
        executor.shutdown();
        while (!executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
            // wait for both bounded test tasks
        }
        if (failure.get() != null && !(failure.get() instanceof RuntimeException)) {
            throw new AssertionError(failure.get());
        }
        return successes;
    }

    @FunctionalInterface
    private interface CheckedOperation {
        void run();
    }
}
