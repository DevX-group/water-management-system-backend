package com.backend.water_management_system.user.repository;

import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByNic(String nic);

    Optional<User> findByEmail(String email);

    boolean existsByNic(String nic);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);
}
