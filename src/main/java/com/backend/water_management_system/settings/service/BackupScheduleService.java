package com.backend.water_management_system.settings.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.water_management_system.settings.dto.BackupResponse;
import com.backend.water_management_system.settings.dto.BackupScheduleRequest;
import com.backend.water_management_system.settings.dto.BackupScheduleResponse;
import com.backend.water_management_system.settings.entity.BackupSchedule;
import com.backend.water_management_system.settings.enums.BackupFrequency;
import com.backend.water_management_system.settings.enums.BackupStatus;
import com.backend.water_management_system.settings.repository.BackupScheduleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupScheduleService {

    private final BackupScheduleRepository scheduleRepository;
    private final BackupService backupService;

    @Transactional(readOnly = true)
    public BackupScheduleResponse getScheduleSettings() {
        BackupSchedule schedule = getOrCreateSchedule();
        return mapToResponse(schedule);
    }

    @Transactional
    public BackupScheduleResponse updateScheduleSettings(BackupScheduleRequest request, String updatedBy) {
        BackupSchedule schedule = getOrCreateSchedule();
        schedule.setFrequency(request.getFrequency());
        schedule.setCreatedBy(updatedBy);
        BackupSchedule saved = scheduleRepository.save(schedule);
        log.info("Backup schedule updated to {} by {}", saved.getFrequency(), updatedBy);
        return mapToResponse(saved);
    }

    @Transactional
    public BackupResponse processCronBackupTrigger(BackupFrequency incomingFrequency) {
        BackupSchedule schedule = getOrCreateSchedule();

        // 1. Check if schedule is disabled in DB
        if (schedule.getFrequency() == BackupFrequency.DISABLE) {
            log.info("Cron trigger '{}' skipped: Backup schedule is DISABLE.", incomingFrequency);
            return BackupResponse.builder()
                    .success(false)
                    .message("Backup schedule is currently DISABLE.")
                    .build();
        }

        // 2. Check if incoming frequency matches active DB setting
        if (schedule.getFrequency() != incomingFrequency) {
            log.info("Cron trigger '{}' skipped: Active setting in DB is '{}'.", incomingFrequency,
                    schedule.getFrequency());
            return BackupResponse.builder()
                    .success(false)
                    .message("Skipped: Active database setting is " + schedule.getFrequency())
                    .build();
        }

        // 3. Mark status as RUNNING
        schedule.setLastBackupStatus(BackupStatus.RUNNING);
        scheduleRepository.save(schedule);

        try {
            // 4. Run `pg_dump`
            BackupResponse response = backupService.createBackup();

            if (response.isSuccess()) {
                schedule.setLastBackupStatus(BackupStatus.SUCCESS);
                schedule.setLastSuccessfulBackupDate(LocalDateTime.now());
                String fileName = (response.getFileInfo() != null) ? response.getFileInfo().getFileName() : "N/A";
                log.info("Scheduled backup completed successfully: {}", fileName);
            } else {
                schedule.setLastBackupStatus(BackupStatus.FAILED);
                log.error("Scheduled backup execution failed: {}", response.getMessage());
            }
            scheduleRepository.save(schedule);
            return response;
        } catch (Exception e) {
            log.error("Exception during scheduled backup execution", e);
            schedule.setLastBackupStatus(BackupStatus.FAILED);
            scheduleRepository.save(schedule);
            return BackupResponse.builder()
                    .success(false)
                    .message("Backup error: " + e.getMessage())
                    .build();
        }
    }

    private BackupSchedule getOrCreateSchedule() {
        return scheduleRepository.findFirstByOrderByIdAsc().orElseGet(() -> {
            BackupSchedule defaultSchedule = BackupSchedule.builder()
                    .frequency(BackupFrequency.DISABLE)
                    .createdBy("SYSTEM")
                    .build();
            return scheduleRepository.save(defaultSchedule);
        });
    }

    private BackupScheduleResponse mapToResponse(BackupSchedule schedule) {
        return BackupScheduleResponse.builder()
                .id(schedule.getId())
                .frequency(schedule.getFrequency())
                .lastBackupStatus(schedule.getLastBackupStatus())
                .lastSuccessfulBackupDate(schedule.getLastSuccessfulBackupDate())
                .createdBy(schedule.getCreatedBy())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
