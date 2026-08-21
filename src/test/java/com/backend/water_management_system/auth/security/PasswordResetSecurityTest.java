package com.backend.water_management_system.auth.security;

import com.backend.water_management_system.auth.config.PasswordResetProperties;
import com.backend.water_management_system.auth.rate.BoundedExpiringPasswordResetRateLimiter;
import com.backend.water_management_system.security.JwtService;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetSecurityTest {

    @Test
    void otpHmacIsDomainSeparatedAndConstantTimeComparable() {
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setOtpHmacSecret("test-only-password-reset-secret");
        PasswordResetCryptography cryptography = new PasswordResetCryptography(properties);

        String first = cryptography.otpHmac("challenge-1", "user-1", "PASSWORD_RESET", "123456");
        String second = cryptography.otpHmac("challenge-1", "user-1", "PASSWORD_RESET", "654321");

        assertThat(first).hasSize(64).isNotEqualTo(second);
        assertThat(cryptography.matches(first, first)).isTrue();
        assertThat(cryptography.matches(first, second)).isFalse();
        assertThat(cryptography.nicRateLimitDigest("200012345678")).isNotEqualTo("200012345678");
    }

    @Test
    void contextDoesNotExposeOtpThroughObjectString() {
        com.backend.water_management_system.auth.delivery.PasswordResetDeliveryContext context =
                new com.backend.water_management_system.auth.delivery.PasswordResetDeliveryContext(
                        java.util.UUID.randomUUID(),
                        com.backend.water_management_system.auth.entity.DeliveryChannel.EMAIL,
                        "123456",
                        java.time.Instant.now().plusSeconds(300),
                        com.backend.water_management_system.auth.entity.PasswordResetPurpose.PASSWORD_RESET);

        assertThat(context.toString()).doesNotContain("123456");
    }

    @Test
    void localLimiterIsBoundedByConfiguredLimit() {
        BoundedExpiringPasswordResetRateLimiter limiter = new BoundedExpiringPasswordResetRateLimiter();
        Duration window = Duration.ofMinutes(15);

        assertThat(limiter.tryAcquire("ip:request:test", 2, window)).isTrue();
        assertThat(limiter.tryAcquire("ip:request:test", 2, window)).isTrue();
        assertThat(limiter.tryAcquire("ip:request:test", 2, window)).isFalse();
    }

    @Test
    void jwtVersionInvalidatesOlderTokenAndAllowsVersionZeroCompatibility() {
        String secret = "0123456789012345678901234567890123456789012345678901234567890123";
        JwtService jwtService = new JwtService(secret, 60_000);
        String token = jwtService.generateToken("200012345678", "CUSTOMER", 0L);

        assertThat(jwtService.isTokenValid(token, 0L)).isTrue();
        assertThat(jwtService.isTokenValid(token, 1L)).isFalse();
    }
}
