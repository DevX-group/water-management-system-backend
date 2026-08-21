package com.backend.water_management_system.auth.repository;

import com.backend.water_management_system.auth.entity.PasswordResetChallenge;
import com.backend.water_management_system.auth.entity.PasswordResetPurpose;
import com.backend.water_management_system.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetChallengeRepository extends JpaRepository<PasswordResetChallenge, UUID> {

    Optional<PasswordResetChallenge> findFirstByUserAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNullAndExpiresAtAfter(
            User user,
            PasswordResetPurpose purpose,
            Instant now
    );

    Optional<PasswordResetChallenge> findFirstByUserAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndInvalidatedAtIsNull(
            User user,
            PasswordResetPurpose purpose,
            Instant now
    );
}
