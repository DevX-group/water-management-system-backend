package com.backend.water_management_system.settings.entity;

import java.time.LocalDateTime;

import com.backend.water_management_system.settings.enums.BackupFrequency;
import com.backend.water_management_system.settings.enums.BackupStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "backup_schedule_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupFrequency frequency; // DISABLE, DAILY, WEEKLY, MONTHLY

    @Enumerated(EnumType.STRING)
    private BackupStatus lastBackupStatus; // RUNNING, SUCCESS, FAILED

    private LocalDateTime lastSuccessfulBackupDate;

    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
