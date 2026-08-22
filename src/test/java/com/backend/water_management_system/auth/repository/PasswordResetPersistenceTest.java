package com.backend.water_management_system.auth.repository;

import com.backend.water_management_system.auth.entity.DeliveryChannel;
import com.backend.water_management_system.auth.entity.PasswordResetChallenge;
import com.backend.water_management_system.auth.entity.PasswordResetAuthorization;
import com.backend.water_management_system.auth.entity.PasswordResetPurpose;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PasswordResetPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetChallengeRepository passwordResetChallengeRepository;

    @Autowired
    private PasswordResetAuthorizationRepository passwordResetAuthorizationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void challengeCanBeSavedAndFoundByActiveLookup() {
        User user = userRepository.save(User.builder()
                .nic("200012345678")
                .email("reset@example.com")
                .fullName("Reset User")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        PasswordResetChallenge challenge = PasswordResetChallenge.builder()
                .user(user)
                .purpose(PasswordResetPurpose.PASSWORD_RESET)
                .otpHmac("test-hmac-value")
                .deliveryChannel(DeliveryChannel.EMAIL)
                .expiresAt(Instant.now().plusSeconds(300))
                .resendAvailableAt(Instant.now())
                .build();

        passwordResetChallengeRepository.save(challenge);

        Optional<PasswordResetChallenge> result = passwordResetChallengeRepository
                .findFirstByUserAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNullAndExpiresAtAfter(
                        user,
                        PasswordResetPurpose.PASSWORD_RESET,
                        Instant.now());

        assertThat(result).isPresent();
        assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void authorizationIsPersistedWithUniqueDigest() {
        User user = userRepository.save(User.builder()
                .nic("199912345678")
                .email("auth@example.com")
                .fullName("Auth User")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        PasswordResetAuthorization authorization = PasswordResetAuthorization.builder()
                .user(user)
                .authorizationDigest("digest-value-123")
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        PasswordResetAuthorization olderAuthorization = PasswordResetAuthorization.builder()
                .user(user)
                .authorizationDigest("digest-value-older")
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        passwordResetAuthorizationRepository.save(authorization);
        passwordResetAuthorizationRepository.save(olderAuthorization);

        Optional<PasswordResetAuthorization> result = passwordResetAuthorizationRepository
                .findByAuthorizationDigest("digest-value-123");

        assertThat(result).isPresent();
        assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(passwordResetAuthorizationRepository
                .findUserIdByAuthorizationDigest("digest-value-123"))
                .contains(user.getId());

        Instant invalidatedAt = Instant.now();
        assertThat(passwordResetAuthorizationRepository
                .invalidateUnusedByUser(user, invalidatedAt))
                .isEqualTo(2);

        entityManager.clear();

        PasswordResetAuthorization invalidated = passwordResetAuthorizationRepository
                .findByAuthorizationDigest("digest-value-123")
                .orElseThrow();
        PasswordResetAuthorization invalidatedOlder = passwordResetAuthorizationRepository
                .findByAuthorizationDigest("digest-value-older")
                .orElseThrow();
        assertThat(invalidated.getUsedAt()).isNotNull();
        assertThat(invalidatedOlder.getUsedAt()).isNotNull();
    }
}
