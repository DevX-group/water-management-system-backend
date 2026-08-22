package com.backend.water_management_system.dashboard.entity;

import com.backend.water_management_system.dashboard.enums.WidgetType;
import com.backend.water_management_system.user.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines a widget type that can be placed on a dashboard.
 *
 * <p>{@code widgetKey} is the stable system identifier (e.g. "customer-current-bill").
 * {@code componentKey} is the frontend registry key used by WidgetRenderer to resolve
 * the React component. These are intentionally separate so the display name can change
 * without breaking existing dashboard configurations.
 */
@Entity
@Table(name = "widget_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WidgetDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable system key — never changes once assigned. Example: "customer-current-bill" */
    @Column(unique = true, nullable = false, length = 100)
    private String widgetKey;

    /** Human-readable display name. */
    @Column(nullable = false, length = 200)
    private String name;

    /** Short description of what the widget shows. */
    @Column(length = 500)
    private String description;

    /** Widget rendering category. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WidgetType widgetType;

    /**
     * Frontend registry key. The WidgetRenderer maps this key to a React component.
     * Must be a known key in the frontend allow-list — never treated as a file path.
     */
    @Column(nullable = false, length = 100)
    private String componentKey;

    /** Whether this widget is enabled. Inactive widgets render as unavailable on the frontend. */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Roles that are authorised to use this widget.
     * Stored as a comma-separated string of Role enum names.
     * A widget with no allowed roles is inaccessible.
     */
    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "widget_allowed_roles", joinColumns = @JoinColumn(name = "widget_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    private Set<Role> allowedRoles = new HashSet<>();

    /** Default grid column span (1–4). Overridden per DashboardWidget placement. */
    @Column(nullable = false)
    @Builder.Default
    private int defaultColSpan = 1;

    /** Default grid row span (1–4). Overridden per DashboardWidget placement. */
    @Column(nullable = false)
    @Builder.Default
    private int defaultRowSpan = 1;

    @Column(nullable = false)
    @Builder.Default
    private int version = 1;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
