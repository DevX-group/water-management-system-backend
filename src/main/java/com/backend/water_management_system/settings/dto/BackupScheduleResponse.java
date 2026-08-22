package com.backend.water_management_system.settings.dto;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.backend.water_management_system.settings.enums.BackupFrequency;
import com.backend.water_management_system.settings.enums.BackupStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupScheduleResponse {
    private Long id;
    private BackupFrequency frequency;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime time;
    private DayOfWeek dayOfWeek;
    private Integer dayOfMonth;
    private String cronExpression;
    private BackupStatus lastBackupStatus;
    private LocalDateTime lastSuccessfulBackupDate;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
