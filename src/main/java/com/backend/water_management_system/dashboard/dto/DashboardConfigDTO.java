package com.backend.water_management_system.dashboard.dto;

import com.backend.water_management_system.user.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Full dashboard configuration returned by {@code GET /api/dashboards/me}.
 * Contains all visible widget placements for the authenticated user's role.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardConfigDTO {
    private Long dashboardId;
    private String dashboardKey;
    private String name;
    private Role assignedRole;
    private int version;
    /** Ordered list of visible widget placements. */
    private List<DashboardWidgetDTO> widgets;
}
