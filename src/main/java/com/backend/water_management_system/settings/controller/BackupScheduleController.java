package com.backend.water_management_system.settings.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.backend.water_management_system.settings.dto.BackupResponse;
import com.backend.water_management_system.settings.dto.BackupScheduleRequest;
import com.backend.water_management_system.settings.dto.BackupScheduleResponse;
import com.backend.water_management_system.settings.enums.BackupFrequency;
import com.backend.water_management_system.settings.service.BackupScheduleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/settings/backups")
@RequiredArgsConstructor
public class BackupScheduleController {

    private final BackupScheduleService backupScheduleService;

    @Value("${backup.cron.secret-key:SuperSecretCronKey123}")
    private String cronSecretKey;

    // Admin Dashboard Endpoints
    @GetMapping("/schedule")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BackupScheduleResponse> getScheduleSettings() {
        return ResponseEntity.ok(backupScheduleService.getScheduleSettings());
    }

    @PutMapping("/schedule")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BackupScheduleResponse> updateScheduleSettings(
            @Valid @RequestBody BackupScheduleRequest request, Principal principal) {
        String adminUser = (principal != null) ? principal.getName() : "ADMIN";
        return ResponseEntity.ok(backupScheduleService.updateScheduleSettings(request, adminUser));
    }

    // External Cron Trigger Endpoint
    @PostMapping("/cron-trigger/{frequency}")
    public ResponseEntity<BackupResponse> handleCronTrigger(
            @PathVariable("frequency") BackupFrequency frequency,
            @RequestHeader(value = "X-CRON-SECRET", required = false) String requestSecret) {

        // Secret validation
        if (requestSecret == null || !requestSecret.equals(cronSecretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BackupResponse.builder().success(false).message("Unauthorized cron secret").build());
        }

        BackupResponse response = backupScheduleService.processCronBackupTrigger(frequency);
        return ResponseEntity.ok(response);
    }
}
