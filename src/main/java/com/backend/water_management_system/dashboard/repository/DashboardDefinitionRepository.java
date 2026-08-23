package com.backend.water_management_system.dashboard.repository;

import com.backend.water_management_system.dashboard.entity.DashboardDefinition;
import com.backend.water_management_system.user.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DashboardDefinitionRepository extends JpaRepository<DashboardDefinition, Long> {

    Optional<DashboardDefinition> findByAssignedRoleAndActiveTrue(Role role);

    Optional<DashboardDefinition> findByDashboardKey(String dashboardKey);

    boolean existsByAssignedRole(Role role);
}
