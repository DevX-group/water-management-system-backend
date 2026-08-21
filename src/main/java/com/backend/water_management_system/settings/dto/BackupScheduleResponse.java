package com.backend.water_management_system.settings.dto;

import java.time.LocalDateTime;

import com.backend.water_management_system.settings.enums.BackupFrequency;
import com.backend.water_management_system.settings.enums.BackupStatus;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupScheduleResponse {
    private Long id;
    private BackupFrequency frequency;
    private BackupStatus lastBackupStatus;
    private LocalDateTime lastSuccessfulBackupDate;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
