package com.backend.water_management_system.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BackupResponse {
    private boolean success;
    private String message;
    private BackupFileInfo fileInfo;
}
