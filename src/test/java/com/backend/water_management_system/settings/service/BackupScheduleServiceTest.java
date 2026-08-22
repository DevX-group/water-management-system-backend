package com.backend.water_management_system.settings.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backend.water_management_system.settings.dto.BackupScheduleResponse;
import com.backend.water_management_system.settings.entity.BackupSchedule;
import com.backend.water_management_system.settings.enums.BackupFrequency;
import com.backend.water_management_system.settings.repository.BackupScheduleRepository;

@ExtendWith(MockitoExtension.class)
class BackupScheduleServiceTest {

    @Mock
    private BackupScheduleRepository scheduleRepository;

    @Mock
    private BackupService backupService;

    @Mock
    private CronJobOrgService cronJobOrgService;

    @InjectMocks
    private BackupScheduleService backupScheduleService;

    @Test
    void testGetScheduleSettings_WhenEmpty_CreatesDefaultSchedule() {
        when(scheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        
        BackupSchedule defaultSavedSchedule = BackupSchedule.builder()
                .id(1L)
                .frequency(BackupFrequency.DISABLE)
                .createdBy("SYSTEM")
                .build();
        
        when(scheduleRepository.save(any(BackupSchedule.class))).thenReturn(defaultSavedSchedule);

        BackupScheduleResponse response = backupScheduleService.getScheduleSettings();

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(BackupFrequency.DISABLE, response.getFrequency());
        assertEquals("SYSTEM", response.getCreatedBy());

        verify(scheduleRepository).save(any(BackupSchedule.class));
    }

    @Test
    void testGetScheduleSettings_WhenExists_ReturnsExistingSchedule() {
        BackupSchedule existingSchedule = BackupSchedule.builder()
                .id(1L)
                .frequency(BackupFrequency.DAILY)
                .createdBy("ADMIN")
                .build();

        when(scheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existingSchedule));

        BackupScheduleResponse response = backupScheduleService.getScheduleSettings();

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(BackupFrequency.DAILY, response.getFrequency());
        assertEquals("ADMIN", response.getCreatedBy());
    }
}
