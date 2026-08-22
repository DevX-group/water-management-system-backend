package com.backend.water_management_system.dashboard.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents the placement and configuration of a widget on a specific dashboard.
 * This is the join between DashboardDefinition and WidgetDefinition.
 *
 * <p>{@code configJson} stores optional per-placement configuration
 * (e.g. chart period, max items). Validated by the service before saving.
 */
@Entity
@Table(
    name = "dashboard_widgets",
    uniqueConstraints = @UniqueConstraint(columnNames = {"dashboard_id", "widget_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardWidget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private DashboardDefinition dashboard;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "widget_id", nullable = false)
    private WidgetDefinition widget;

    /** Order the widget appears in (lower = first). */
    @Column(nullable = false)
    @Builder.Default
    private int position = 0;

    /** Number of grid columns this widget occupies (1–4). */
    @Column(nullable = false)
    @Builder.Default
    private int colSpan = 1;

    /** Number of grid rows this widget occupies (1–4). */
    @Column(nullable = false)
    @Builder.Default
    private int rowSpan = 1;

    /** Whether the widget is shown (true) or hidden (false) on this dashboard. */
    @Column(nullable = false)
    @Builder.Default
    private boolean visible = true;

    /**
     * Optional JSON configuration for this widget placement.
     * Examples: {"maxItems":5}, {"chartPeriod":"YEAR"}, {"showDetails":true}
     * Validated by WidgetService before persistence.
     */
    @Column(columnDefinition = "TEXT")
    private String configJson;
}
