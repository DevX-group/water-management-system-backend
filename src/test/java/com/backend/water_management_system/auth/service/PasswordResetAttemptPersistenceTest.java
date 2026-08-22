package com.backend.water_management_system.auth.service;

import com.backend.water_management_system.auth.config.PasswordResetProperties;
import com.backend.water_management_system.auth.delivery.PasswordResetDeliveryService;
import com.backend.water_management_system.auth.dto.PasswordResetVerifyRequest;
import com.backend.water_management_system.auth.entity.DeliveryChannel;
import com.backend.water_management_system.auth.entity.PasswordResetChallenge;
import com.backend.water_management_system.auth.entity.PasswordResetPurpose;
import com.backend.water_management_system.auth.exception.PasswordResetException;
import com.backend.water_management_system.auth.rate.PasswordResetRateLimiter;
import com.backend.water_management_system.auth.repository.PasswordResetChallengeRepository;
import com.backend.water_management_system.auth.security.PasswordResetCryptography;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        PasswordResetService.class,
        PasswordResetCryptography.class,
        PasswordResetAttemptPersistenceTest.TestConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PasswordResetAttemptPersistenceTest {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordResetCryptography cryptography;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetChallengeRepository challengeRepository;

    @MockitoBean
    private PasswordResetRateLimiter rateLimiter;

    @MockitoBean
    private PasswordResetDeliveryService deliveryService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        PasswordResetProperties passwordResetProperties() {
            PasswordResetProperties properties = new PasswordResetProperties();
            properties.setOtpHmacSecret("test-only-password-reset-secret");
            return properties;
        }

        @Bean(name = "passwordResetDeliveryExecutor")
        Executor passwordResetDeliveryExecutor() {
            return Runnable::run;
        }
    }

    @Test
    void failedOtpAttemptsPersistAndInvalidateChallengeAtMaximum() {
        org.mockito.Mockito.when(rateLimiter.tryAcquire(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(true);

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String nic = "2099" + String.format(
                "%08d",
                Integer.toUnsignedLong(suffix.hashCode()) % 100_000_000L
        );
        User user = userRepository.save(User.builder()
                .nic(nic)
                .email("otp-attempt-" + suffix + "@example.com")
                .fullName("OTP Attempt Test")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        PasswordResetChallenge challenge = PasswordResetChallenge.builder()
                .id(UUID.randomUUID())
                .user(user)
                .purpose(PasswordResetPurpose.PASSWORD_RESET)
                .deliveryChannel(DeliveryChannel.EMAIL)
                .expiresAt(Instant.now().plusSeconds(300))
                .resendAvailableAt(Instant.now())
                .build();
        challenge.setOtpHmac(cryptography.otpHmac(
                challenge.getId().toString(),
                user.getId().toString(),
                PasswordResetPurpose.PASSWORD_RESET.name(),
                "123456"
        ));
        challenge = challengeRepository.save(challenge);

        try {
            for (int attempt = 0; attempt < 5; attempt++) {
                assertThatThrownBy(() -> passwordResetService.verify(
                        new PasswordResetVerifyRequest(user.getNic(), "000000"),
                        "192.0.2.50"
                ))
                        .isInstanceOf(PasswordResetException.class)
                        .hasMessage(PasswordResetService.INVALID_CODE_MESSAGE);
            }

            PasswordResetChallenge persisted = challengeRepository
                    .findById(challenge.getId())
                    .orElseThrow();

            assertThat(persisted.getFailedAttempts()).isEqualTo(5);
            assertThat(persisted.getInvalidatedAt()).isNotNull();
        } finally {
            challengeRepository.deleteById(challenge.getId());
            userRepository.deleteById(user.getId());
        }
    }
}
