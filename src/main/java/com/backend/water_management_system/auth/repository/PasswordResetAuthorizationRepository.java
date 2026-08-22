package com.backend.water_management_system.auth.repository;

import com.backend.water_management_system.auth.entity.PasswordResetAuthorization;
import com.backend.water_management_system.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetAuthorizationRepository extends JpaRepository<PasswordResetAuthorization, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetAuthorization> findByAuthorizationDigest(String authorizationDigest);

    @Query("select a.user.id from PasswordResetAuthorization a where a.authorizationDigest = :authorizationDigest")
    Optional<UUID> findUserIdByAuthorizationDigest(
            @Param("authorizationDigest") String authorizationDigest
    );

    @Modifying(flushAutomatically = true)
    @Query("update PasswordResetAuthorization a set a.usedAt = :now "
            + "where a.user = :user and a.usedAt is null")
    int invalidateUnusedByUser(
            @Param("user") User user,
            @Param("now") Instant now
    );

    Optional<PasswordResetAuthorization> findFirstByUserAndUsedAtIsNullAndExpiresAtAfter(User user, Instant now);
}
