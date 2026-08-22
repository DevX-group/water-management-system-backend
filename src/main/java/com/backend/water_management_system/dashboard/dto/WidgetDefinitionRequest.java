package com.backend.water_management_system.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body for creating or updating a widget definition. Super Admin only. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WidgetDefinitionRequest {

    @NotNull
    @Size(min = 3, max = 100)
    private String widgetKey;

    @NotNull
    @Size(min = 2, max = 200)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    private String widgetType; // validated against WidgetType enum in service

    @NotNull
    @Size(min = 3, max = 100)
    private String componentKey;

    private boolean active = true;

    private java.util.Set<String> allowedRoles; // validated against Role enum in service

    @Min(1) @Max(4)
    private int defaultColSpan = 1;

    @Min(1) @Max(4)
    private int defaultRowSpan = 1;
}
