package com.backend.water_management_system.auth.repository;

import com.backend.water_management_system.auth.entity.PasswordResetAuthorization;
import com.backend.water_management_system.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetAuthorizationRepository extends JpaRepository<PasswordResetAuthorization, UUID> {

    Optional<PasswordResetAuthorization> findByAuthorizationDigest(String authorizationDigest);

    Optional<PasswordResetAuthorization> findFirstByUserAndUsedAtIsNullAndExpiresAtAfter(User user, Instant now);
}
