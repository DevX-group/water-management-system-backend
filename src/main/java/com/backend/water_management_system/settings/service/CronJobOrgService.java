package com.backend.water_management_system.settings.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.backend.water_management_system.settings.enums.BackupFrequency;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CronJobOrgService {

    @Value("${app.cron-job-org.api-key}")
    private String apiKey;

    @Value("${app.cron-job-org.job-id}")
    private String jobId;

    private static final String API_URL = "https://api.cron-job.org/jobs/";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void updateCronJobSchedule(BackupFrequency frequency, LocalTime time, DayOfWeek dayOfWeek,
            Integer dayOfMonth) {
        if (apiKey == null || apiKey.trim().isEmpty() || jobId == null || jobId.trim().isEmpty()) {
            log.error("CRON_JOB_ORG_API_KEY or CRON_JOB_ORG_BACKUP_JOB_ID is not configured in environment settings.");
            throw new IllegalStateException("cron-job.org API key or Backup Job ID is not configured.");
        }

        try {
            boolean enabled = (frequency != BackupFrequency.DISABLE);
            List<Integer> hours = (frequency != BackupFrequency.DISABLE && time != null) ? List.of(time.getHour())
                    : List.of(-1);
            List<Integer> minutes = (frequency != BackupFrequency.DISABLE && time != null) ? List.of(time.getMinute())
                    : List.of(0);
            List<Integer> mdays = (frequency == BackupFrequency.MONTHLY && dayOfMonth != null) ? List.of(dayOfMonth)
                    : List.of(-1);
            List<Integer> months = List.of(-1);
            List<Integer> wdays = (frequency == BackupFrequency.WEEKLY && dayOfWeek != null)
                    ? List.of(mapDayOfWeekToCronWday(dayOfWeek))
                    : List.of(-1);

            Map<String, Object> scheduleMap = new HashMap<>();
            scheduleMap.put("timezone", "Asia/Colombo");
            scheduleMap.put("expiresAt", 0);
            scheduleMap.put("hours", hours);
            scheduleMap.put("minutes", minutes);
            scheduleMap.put("mdays", mdays);
            scheduleMap.put("months", months);
            scheduleMap.put("wdays", wdays);

            Map<String, Object> jobMap = new HashMap<>();
            jobMap.put("enabled", enabled);
            jobMap.put("schedule", scheduleMap);

            Map<String, Object> payload = Map.of("job", jobMap);
            String jsonPayload = objectMapper.writeValueAsString(payload);

            String targetUrl = API_URL + jobId.trim();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            log.info("Sending PATCH request to cron-job.org for job ID {}: {}", jobId, jsonPayload);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Successfully updated cron-job.org schedule. Response code: {}", response.statusCode());
            } else {
                log.error("Failed to update cron-job.org schedule. Status: {}, Body: {}", response.statusCode(),
                        response.body());
                throw new IllegalStateException("Failed to update cron-job.org schedule. Status: "
                        + response.statusCode() + ", Message: " + response.body());
            }
        } catch (Exception e) {
            log.error("Error updating schedule on cron-job.org", e);
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new RuntimeException("Error communicating with cron-job.org: " + e.getMessage(), e);
        }
    }

    private int mapDayOfWeekToCronWday(DayOfWeek dayOfWeek) {
        // cron-job.org: Sunday=0, Monday=1, Tuesday=2, Wednesday=3, Thursday=4,
        // Friday=5, Saturday=6
        // Java DayOfWeek: Monday=1 ... Sunday=7
        return (dayOfWeek == DayOfWeek.SUNDAY) ? 0 : dayOfWeek.getValue();
    }
}
