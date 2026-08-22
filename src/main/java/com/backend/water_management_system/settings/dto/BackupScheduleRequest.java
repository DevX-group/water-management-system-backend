package com.backend.water_management_system.settings.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.backend.water_management_system.settings.enums.BackupFrequency;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupScheduleRequest {

    @NotNull(message = "Frequency is required")
    private BackupFrequency frequency;

    @JsonFormat(pattern = "HH:mm[:ss]")
    private LocalTime time;

    private DayOfWeek dayOfWeek;

    private Integer dayOfMonth;
}
