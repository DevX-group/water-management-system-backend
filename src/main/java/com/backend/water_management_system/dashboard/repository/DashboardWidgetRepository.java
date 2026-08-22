package com.backend.water_management_system.dashboard.repository;

import com.backend.water_management_system.dashboard.entity.DashboardWidget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {

    /** Returns all visible widget placements for a dashboard, ordered by position. */
    List<DashboardWidget> findByDashboard_IdAndVisibleTrueOrderByPositionAsc(Long dashboardId);

    /** Returns all widget placements for a dashboard (including hidden), ordered by position. */
    List<DashboardWidget> findByDashboard_IdOrderByPositionAsc(Long dashboardId);

    /** Checks if a specific widget is already on a dashboard. */
    boolean existsByDashboard_IdAndWidget_Id(Long dashboardId, Long widgetId);

    void deleteByDashboard_Id(Long dashboardId);
}
