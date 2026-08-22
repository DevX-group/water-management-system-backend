package com.backend.water_management_system.user.repository;

import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByNic(String nic);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.nic = :nic")
    Optional<User> findLockedByNic(@Param("nic") String nic);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findLockedById(@Param("id") UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByNic(String nic);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    java.util.List<User> findAllByRole(Role role);

    /** Count users by role — used by dashboard aggregation. */
    long countByRole(Role role);
}
