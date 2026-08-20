package com.backend.water_management_system.settings.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.water_management_system.common.config.SecurityConfig;
import com.backend.water_management_system.security.CustomUserDetailsService;
import com.backend.water_management_system.security.JwtService;
import com.backend.water_management_system.settings.dto.BackupFileInfo;
import com.backend.water_management_system.settings.dto.BackupResponse;
import com.backend.water_management_system.settings.service.BackupService;

import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(BackupController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "SUPER_ADMIN")
class BackupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackupService backupService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void testCreateBackup_Success() throws Exception {
        BackupFileInfo info = BackupFileInfo.builder()
                .fileName("backup_20260819_120000.sql")
                .sizeBytes(1024)
                .formattedSize("1.00 KB")
                .createdAt(LocalDateTime.now())
                .downloadUrl("/api/settings/backups/download/backup_20260819_120000.sql")
                .build();

        BackupResponse response = BackupResponse.builder()
                .success(true)
                .message("Backup created successfully.")
                .fileInfo(info)
                .build();

        when(backupService.createBackup()).thenReturn(response);

        mockMvc.perform(post("/api/settings/backups"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.fileInfo.fileName").value("backup_20260819_120000.sql"));

        verify(backupService, times(1)).createBackup();
    }

    @Test
    void testListBackups_Success() throws Exception {
        BackupFileInfo info = BackupFileInfo.builder()
                .fileName("backup_1.sql")
                .sizeBytes(2048)
                .formattedSize("2.00 KB")
                .createdAt(LocalDateTime.now())
                .downloadUrl("/api/settings/backups/download/backup_1.sql")
                .build();

        when(backupService.listBackups()).thenReturn(List.of(info));

        mockMvc.perform(get("/api/settings/backups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("backup_1.sql"));

        verify(backupService, times(1)).listBackups();
    }

    @Test
    void testDownloadBackup_Success() throws Exception {
        Resource resource = new ByteArrayResource("SELECT 1;".getBytes()) {
            @Override
            public String getFilename() {
                return "backup_1.sql";
            }
        };

        when(backupService.getBackupFileResource("backup_1.sql")).thenReturn(resource);

        mockMvc.perform(get("/api/settings/backups/download/backup_1.sql"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"backup_1.sql\""));

        verify(backupService, times(1)).getBackupFileResource("backup_1.sql");
    }

    @Test
    void testDeleteBackup_Success() throws Exception {
        doNothing().when(backupService).deleteBackup(anyString());

        mockMvc.perform(delete("/api/settings/backups/backup_1.sql"))
                .andExpect(status().isNoContent());

        verify(backupService, times(1)).deleteBackup("backup_1.sql");
    }

    @Test
    void testRestoreBackup_Success() throws Exception {
        BackupResponse response = BackupResponse.builder()
                .success(true)
                .message("Database restored successfully.")
                .build();

        when(backupService.restoreBackup("backup_1.sql")).thenReturn(response);

        mockMvc.perform(post("/api/settings/backups/restore/backup_1.sql"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(backupService, times(1)).restoreBackup("backup_1.sql");
    }
}
