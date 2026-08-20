package com.backend.water_management_system.settings.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.backend.water_management_system.settings.dto.BackupFileInfo;
import com.backend.water_management_system.settings.dto.BackupResponse;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BackupService {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.backup.directory:./backups}")
    private String backupDirectoryPath;

    @Value("${app.backup.pg-dump-path:pg_dump}")
    private String pgDumpExecutable;

    @Value("${app.backup.psql-path:psql}")
    private String psqlExecutable;

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Pattern DB_URL_PATTERN = Pattern.compile("jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^?]+)");

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(backupDirectoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created backup directory at: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to initialize backup directory", e);
        }
    }

    public BackupResponse createBackup() {
        DbConnDetails connDetails = parseJdbcUrl(datasourceUrl);
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMATTER);
        String fileName = "backup_" + timestamp + ".sql";

        Path backupDirPath = Paths.get(backupDirectoryPath);
        File backupFile = backupDirPath.resolve(fileName).toFile();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pgDumpExecutable,
                    "-h", connDetails.host,
                    "-p", String.valueOf(connDetails.port),
                    "-U", dbUsername,
                    "-d", connDetails.databaseName,
                    "-F", "p");

            if (dbPassword != null && !dbPassword.isBlank()) {
                pb.environment().put("PGPASSWORD", dbPassword);
            }

            pb.redirectOutput(backupFile);
            Process process = pb.start();

            String errorMsg = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                if (backupFile.exists()) {
                    backupFile.delete();
                }
                log.error("pg_dump process failed with exit code {}: {}", exitCode, errorMsg);
                return BackupResponse.builder()
                        .success(false)
                        .message("Backup process failed: " + (errorMsg.isBlank() ? "Exit code " + exitCode : errorMsg))
                        .build();
            }

            BackupFileInfo fileInfo = buildBackupFileInfo(backupFile);
            log.info("Backup successfully created: {}", fileName);

            return BackupResponse.builder()
                    .success(true)
                    .message("Backup created successfully.")
                    .fileInfo(fileInfo)
                    .build();

        } catch (IOException e) {
            log.error("IO Exception during backup execution", e);
            if (backupFile.exists()) {
                backupFile.delete();
            }
            return BackupResponse.builder()
                    .success(false)
                    .message(
                            "Failed to execute pg_dump process. Please ensure PostgreSQL client tools are installed and added to PATH: "
                                    + e.getMessage())
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Backup execution interrupted", e);
            return BackupResponse.builder()
                    .success(false)
                    .message("Backup process was interrupted.")
                    .build();
        }
    }

    public List<BackupFileInfo> listBackups() {
        List<BackupFileInfo> list = new ArrayList<>();
        File folder = Paths.get(backupDirectoryPath).toFile();

        if (!folder.exists() || !folder.isDirectory()) {
            return list;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".sql") || name.endsWith(".dump"));
        if (files != null) {
            for (File file : files) {
                list.add(buildBackupFileInfo(file));
            }
        }

        list.sort(Comparator.comparing(BackupFileInfo::getCreatedAt).reversed());
        return list;
    }

    public Resource getBackupFileResource(String fileName) {
        validateFileName(fileName);
        Path filePath = Paths.get(backupDirectoryPath).resolve(fileName).normalize();

        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Backup file not found: " + fileName);
        }

        return new FileSystemResource(file);
    }

    public void deleteBackup(String fileName) {
        validateFileName(fileName);
        Path filePath = Paths.get(backupDirectoryPath).resolve(fileName).normalize();

        File file = filePath.toFile();
        if (!file.exists()) {
            throw new IllegalArgumentException("Backup file not found: " + fileName);
        }

        if (!file.delete()) {
            throw new RuntimeException("Failed to delete backup file: " + fileName);
        }
        log.info("Deleted backup file: {}", fileName);
    }

    public BackupResponse restoreBackup(String fileName) {
        validateFileName(fileName);
        Path filePath = Paths.get(backupDirectoryPath).resolve(fileName).normalize();
        File backupFile = filePath.toFile();

        if (!backupFile.exists()) {
            throw new IllegalArgumentException("Backup file not found: " + fileName);
        }

        DbConnDetails connDetails = parseJdbcUrl(datasourceUrl);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    psqlExecutable,
                    "-h", connDetails.host,
                    "-p", String.valueOf(connDetails.port),
                    "-U", dbUsername,
                    "-d", connDetails.databaseName,
                    "-f", backupFile.getAbsolutePath());

            if (dbPassword != null && !dbPassword.isBlank()) {
                pb.environment().put("PGPASSWORD", dbPassword);
            }

            Process process = pb.start();
            String errorMsg = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("psql restore process failed with exit code {}: {}", exitCode, errorMsg);
                return BackupResponse.builder()
                        .success(false)
                        .message("Restore failed: " + (errorMsg.isBlank() ? "Exit code " + exitCode : errorMsg))
                        .build();
            }

            log.info("Database successfully restored from file: {}", fileName);
            return BackupResponse.builder()
                    .success(true)
                    .message("Database restored successfully from " + fileName)
                    .fileInfo(buildBackupFileInfo(backupFile))
                    .build();

        } catch (IOException e) {
            log.error("IO Exception during restore execution", e);
            return BackupResponse.builder()
                    .success(false)
                    .message("Failed to execute psql restore command: " + e.getMessage())
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Restore process interrupted", e);
            return BackupResponse.builder()
                    .success(false)
                    .message("Restore process was interrupted.")
                    .build();
        }
    }

    private void validateFileName(String fileName) {
        if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Invalid file name provided.");
        }
    }

    private BackupFileInfo buildBackupFileInfo(File file) {
        long bytes = file.length();
        LocalDateTime createdAt = LocalDateTime.now();

        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            createdAt = LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            createdAt = LocalDateTime
                    .ofInstant(file.lastModified() > 0 ? java.time.Instant.ofEpochMilli(file.lastModified())
                            : java.time.Instant.now(), ZoneId.systemDefault());
        }

        return BackupFileInfo.builder()
                .fileName(file.getName())
                .sizeBytes(bytes)
                .formattedSize(formatFileSize(bytes))
                .createdAt(createdAt)
                .downloadUrl("/api/settings/backups/download/" + file.getName())
                .build();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %cB", bytes / Math.pow(1024, exp), pre);
    }

    private DbConnDetails parseJdbcUrl(String url) {
        if (url == null || url.isBlank()) {
            return new DbConnDetails("localhost", 5432, "postgres");
        }

        Matcher matcher = DB_URL_PATTERN.matcher(url);
        if (matcher.find()) {
            String host = matcher.group(1);
            String portStr = matcher.group(2);
            String dbName = matcher.group(3);

            int port = (portStr != null && !portStr.isBlank()) ? Integer.parseInt(portStr) : 5432;
            return new DbConnDetails(host, port, dbName);
        }

        return new DbConnDetails("localhost", 5432, "postgres");
    }

    private record DbConnDetails(String host, int port, String databaseName) {
    }
}
