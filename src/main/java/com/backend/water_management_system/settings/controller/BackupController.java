package com.backend.water_management_system.settings.controller;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.settings.dto.BackupFileInfo;
import com.backend.water_management_system.settings.dto.BackupResponse;
import com.backend.water_management_system.settings.service.BackupService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/settings/backups")
@CrossOrigin(origins = "http://localhost:8080")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @PostMapping
    public ResponseEntity<BackupResponse> createBackup() {
        BackupResponse response = backupService.createBackup();
        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<List<BackupFileInfo>> listBackups() {
        List<BackupFileInfo> backups = backupService.listBackups();
        return ResponseEntity.ok(backups);
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String fileName) {
        Resource resource = backupService.getBackupFileResource(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{fileName:.+}")
    public ResponseEntity<Void> deleteBackup(@PathVariable String fileName) {
        backupService.deleteBackup(fileName);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restore/{fileName:.+}")
    public ResponseEntity<BackupResponse> restoreBackup(@PathVariable String fileName) {
        BackupResponse response = backupService.restoreBackup(fileName);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
