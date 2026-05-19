package com.backend.water_management_system.user.repository;

import com.backend.water_management_system.user.entity.ActivationToken;
import com.backend.water_management_system.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, UUID> {

    Optional<ActivationToken> findByToken(String token);

    // Find the latest valid (unused) token for a given user
    Optional<ActivationToken> findByUserAndUsedFalse(User user);
}
