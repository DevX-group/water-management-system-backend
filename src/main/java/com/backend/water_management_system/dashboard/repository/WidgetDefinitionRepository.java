package com.backend.water_management_system.dashboard.repository;

import com.backend.water_management_system.dashboard.entity.WidgetDefinition;
import com.backend.water_management_system.user.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WidgetDefinitionRepository extends JpaRepository<WidgetDefinition, Long> {

    Optional<WidgetDefinition> findByWidgetKey(String widgetKey);

    boolean existsByWidgetKey(String widgetKey);

    List<WidgetDefinition> findByActiveTrue();

    /** Finds all active widgets whose allowedRoles contain the given role. */
    @Query("SELECT w FROM WidgetDefinition w JOIN w.allowedRoles r WHERE r = :role AND w.active = true")
    List<WidgetDefinition> findActiveWidgetsByRole(@Param("role") Role role);
}
