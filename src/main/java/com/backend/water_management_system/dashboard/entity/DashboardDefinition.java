package com.backend.water_management_system.dashboard.entity;

import com.backend.water_management_system.user.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a configured dashboard for a particular role.
 * Each role has exactly one active default dashboard.
 */
@Entity
@Table(name = "dashboard_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable system key, e.g. "dashboard-customer", "dashboard-super-admin". */
    @Column(unique = true, nullable = false, length = 100)
    private String dashboardKey;

    /** Human-readable name. */
    @Column(nullable = false, length = 200)
    private String name;

    /** The role this dashboard is presented to. One dashboard per role. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role assignedRole;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean defaultDashboard = true;

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
