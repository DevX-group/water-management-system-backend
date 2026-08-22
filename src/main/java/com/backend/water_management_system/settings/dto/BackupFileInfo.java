package com.backend.water_management_system.settings.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BackupFileInfo {
    private String fileName;
    private long sizeBytes;
    private String formattedSize;
    private LocalDateTime createdAt;
    private String downloadUrl;
}
