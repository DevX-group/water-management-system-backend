package com.backend.water_management_system.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single widget placement on a dashboard, returned as part of
 * {@link DashboardConfigDTO}. The {@code componentKey} is used by the frontend
 * WidgetRenderer to resolve the correct React component.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardWidgetDTO {
    private Long id;
    private String widgetKey;
    private String name;
    private String description;
    private String widgetType;
    /** Frontend component registry key — safe allow-listed value. */
    private String componentKey;
    private int position;
    private int colSpan;
    private int rowSpan;
    private boolean visible;
    /** Optional JSON configuration string for this placement. May be null. */
    private String configJson;
}
