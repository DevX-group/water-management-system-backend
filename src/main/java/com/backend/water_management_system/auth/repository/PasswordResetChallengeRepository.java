package com.backend.water_management_system.auth.repository;

import com.backend.water_management_system.auth.entity.PasswordResetChallenge;
import com.backend.water_management_system.auth.entity.PasswordResetPurpose;
import com.backend.water_management_system.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetChallengeRepository extends JpaRepository<PasswordResetChallenge, UUID> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        Optional<PasswordResetChallenge> findFirstByUserAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            User user,
            PasswordResetPurpose purpose,
            Instant now
    );

            @Lock(LockModeType.PESSIMISTIC_WRITE)
            Optional<PasswordResetChallenge> findFirstByUserAndPurposeAndUsedAtIsNullAndInvalidatedAtIsNullAndExpiresAtAfter(
                User user,
                PasswordResetPurpose purpose,
                Instant now
            );

        Optional<PasswordResetChallenge> findFirstByUserAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndInvalidatedAtIsNullOrderByCreatedAtDesc(
            User user,
            PasswordResetPurpose purpose,
            Instant now
    );

        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.data.jpa.repository.Query("update PasswordResetChallenge c set c.invalidatedAt = :now where c.user = :user and c.purpose = :purpose and c.usedAt is null and c.invalidatedAt is null")
        int invalidateActiveByUserAndPurpose(
                        @org.springframework.data.repository.query.Param("user") User user,
                        @org.springframework.data.repository.query.Param("purpose") PasswordResetPurpose purpose,
                        @org.springframework.data.repository.query.Param("now") Instant now
        );
}
