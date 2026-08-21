package com.backend.water_management_system.settings.dto;

import com.backend.water_management_system.settings.enums.BackupFrequency;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupScheduleRequest {

    @NotNull(message = "Frequency is required")
    private BackupFrequency frequency;
}
