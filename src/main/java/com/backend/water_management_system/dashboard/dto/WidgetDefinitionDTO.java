package com.backend.water_management_system.dashboard.dto;

import com.backend.water_management_system.dashboard.enums.WidgetType;
import com.backend.water_management_system.user.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/** Compact representation of a widget definition for catalog and config responses. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WidgetDefinitionDTO {
    private Long id;
    private String widgetKey;
    private String name;
    private String description;
    private WidgetType widgetType;
    private String componentKey;
    private boolean active;
    private Set<Role> allowedRoles;
    private int defaultColSpan;
    private int defaultRowSpan;
    private int version;
}
