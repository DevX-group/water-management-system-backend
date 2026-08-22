package com.backend.water_management_system.settings.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import com.backend.water_management_system.settings.dto.BackupFileInfo;

class BackupServiceTest {

    private BackupService backupService;

    @TempDir
    Path tempBackupDir;

    @BeforeEach
    void setUp() {
        backupService = new BackupService();
        ReflectionTestUtils.setField(backupService, "backupDirectoryPath", tempBackupDir.toString());
        ReflectionTestUtils.setField(backupService, "datasourceUrl", "jdbc:postgresql://localhost:5432/test_db");
        ReflectionTestUtils.setField(backupService, "dbUsername", "postgres");
        ReflectionTestUtils.setField(backupService, "dbPassword", "password");
        ReflectionTestUtils.setField(backupService, "pgDumpExecutable", "pg_dump");
        ReflectionTestUtils.setField(backupService, "pgRestoreExecutable", "pg_restore");
        ReflectionTestUtils.setField(backupService, "psqlExecutable", "psql");

        backupService.init();
    }

    @Test
    void testListBackups_EmptyDirectory() {
        List<BackupFileInfo> backups = backupService.listBackups();
        assertNotNull(backups);
        assertTrue(backups.isEmpty());
    }

    @Test
    void testListBackups_WithFiles() throws IOException {
        File dummySql = tempBackupDir.resolve("backup_20260819_120000.sql").toFile();
        File dummyDump = tempBackupDir.resolve("backup_20260819_130000.dump").toFile();
        assertTrue(dummySql.createNewFile());
        assertTrue(dummyDump.createNewFile());

        List<BackupFileInfo> backups = backupService.listBackups();
        assertEquals(2, backups.size());
    }

    @Test
    void testGetBackupFileResource_Success() throws IOException {
        File dummyFile = tempBackupDir.resolve("backup_test.sql").toFile();
        assertTrue(dummyFile.createNewFile());

        Resource resource = backupService.getBackupFileResource("backup_test.sql");
        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void testGetBackupFileResource_InvalidFileName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> backupService.getBackupFileResource("../etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> backupService.getBackupFileResource("sub/folder/file.sql"));
    }

    @Test
    void testDeleteBackup_Success() throws IOException {
        File dummyFile = tempBackupDir.resolve("backup_to_delete.sql").toFile();
        assertTrue(dummyFile.createNewFile());
        assertTrue(dummyFile.exists());

        backupService.deleteBackup("backup_to_delete.sql");
        assertFalse(dummyFile.exists());
    }

    @Test
    void testDeleteBackup_NotFound_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> backupService.deleteBackup("non_existent.sql"));
    }
}
