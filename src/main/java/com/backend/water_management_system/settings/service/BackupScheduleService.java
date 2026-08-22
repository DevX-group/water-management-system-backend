package com.backend.water_management_system.settings.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
    private final CronJobOrgService cronJobOrgService;

    @Transactional
    public BackupScheduleResponse getScheduleSettings() {
        BackupSchedule schedule = getOrCreateSchedule();
        return mapToResponse(schedule);
    }

    @Transactional
    public BackupScheduleResponse updateScheduleSettings(BackupScheduleRequest request, String updatedBy) {
        BackupFrequency frequency = request.getFrequency();
        LocalTime time = request.getTime();
        DayOfWeek dayOfWeek = request.getDayOfWeek();
        Integer dayOfMonth = request.getDayOfMonth();

        // 1. Validation logic based on frequency
        if (frequency == BackupFrequency.DISABLE) {
            time = null;
            dayOfWeek = null;
            dayOfMonth = null;
        } else if (frequency == BackupFrequency.DAILY) {
            if (time == null) {
                throw new IllegalArgumentException("Time is required for daily backup schedule.");
            }
            dayOfWeek = null;
            dayOfMonth = null;
        } else if (frequency == BackupFrequency.WEEKLY) {
            if (time == null) {
                throw new IllegalArgumentException("Time is required for weekly backup schedule.");
            }
            if (dayOfWeek == null) {
                throw new IllegalArgumentException("Day of week is required for weekly backup schedule.");
            }
            dayOfMonth = null;
        } else if (frequency == BackupFrequency.MONTHLY) {
            if (time == null) {
                throw new IllegalArgumentException("Time is required for monthly backup schedule.");
            }
            if (dayOfMonth == null || dayOfMonth < 1 || dayOfMonth > 31) {
                throw new IllegalArgumentException(
                        "Valid day of month (1-31) is required for monthly backup schedule.");
            }
            dayOfWeek = null;
        }

        // 2. Generate cron expression
        String cronExpression = generateCronExpression(frequency, time, dayOfWeek, dayOfMonth);

        // 3. Update external cron job on cron-job.org
        cronJobOrgService.updateCronJobSchedule(frequency, time, dayOfWeek, dayOfMonth);

        // 4. Save entity to database
        BackupSchedule schedule = getOrCreateSchedule();
        schedule.setFrequency(frequency);
        schedule.setTime(time);
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setDayOfMonth(dayOfMonth);
        schedule.setCronExpression(cronExpression);
        schedule.setCreatedBy(updatedBy);

        BackupSchedule saved = scheduleRepository.save(schedule);
        log.info("Backup schedule updated to frequency: {}, cron: {} by {}", frequency, cronExpression, updatedBy);
        return mapToResponse(saved);
    }

    @Transactional
    public BackupResponse processCronBackupTrigger() {
        BackupSchedule schedule = getOrCreateSchedule();

        // Check if schedule is disabled in DB
        if (schedule.getFrequency() == BackupFrequency.DISABLE) {
            log.info("Cron trigger skipped: Backup schedule is DISABLE.");
            return BackupResponse.builder()
                    .success(false)
                    .message("Backup schedule is currently DISABLE.")
                    .build();
        }

        // Mark status as RUNNING
        schedule.setLastBackupStatus(BackupStatus.RUNNING);
        scheduleRepository.save(schedule);

        try {
            // Run backup
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

    private String generateCronExpression(BackupFrequency frequency, LocalTime time, DayOfWeek dayOfWeek,
            Integer dayOfMonth) {
        if (frequency == BackupFrequency.DISABLE || time == null) {
            return null;
        }
        int min = time.getMinute();
        int hour = time.getHour();

        switch (frequency) {
            case DAILY:
                return String.format("%d %d * * *", min, hour);
            case WEEKLY:
                // Cron weekday: 0=Sun, 1=Mon, ..., 6=Sat
                int cronWday = (dayOfWeek == DayOfWeek.SUNDAY) ? 0 : dayOfWeek.getValue();
                return String.format("%d %d * * %d", min, hour, cronWday);
            case MONTHLY:
                return String.format("%d %d %d * *", min, hour, dayOfMonth);
            default:
                return null;
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
                .time(schedule.getTime())
                .dayOfWeek(schedule.getDayOfWeek())
                .dayOfMonth(schedule.getDayOfMonth())
                .cronExpression(schedule.getCronExpression())
                .lastBackupStatus(schedule.getLastBackupStatus())
                .lastSuccessfulBackupDate(schedule.getLastSuccessfulBackupDate())
                .createdBy(schedule.getCreatedBy())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
