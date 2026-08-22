package com.backend.water_management_system.settings.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.backend.water_management_system.payments.dto.CloudinaryUploadResponse;
import com.backend.water_management_system.payments.service.CloudinaryService;
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

    @Value("${app.backup.pg-restore-path:pg_restore}")
    private String pgRestoreExecutable;

    @Value("${app.backup.psql-path:psql}")
    private String psqlExecutable;

    @Autowired(required = false)
    private CloudinaryService cloudinaryService;

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
        String fileName = "backup_" + timestamp + ".dump";

        Path backupDirPath = Paths.get(backupDirectoryPath);
        File backupFile = backupDirPath.resolve(fileName).toFile();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pgDumpExecutable,
                    "-h", connDetails.host,
                    "-p", String.valueOf(connDetails.port),
                    "-U", dbUsername,
                    "-d", connDetails.databaseName,
                    "-F", "c",
                    "--no-owner",
                    "--no-acl");

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

            log.info("Local backup snapshot generated successfully: {}", fileName);
            BackupFileInfo fileInfo = buildBackupFileInfo(backupFile);

            // Delegated to CloudinaryService
            if (isCloudinaryAvailable()) {
                try {
                    CloudinaryUploadResponse uploadResult = cloudinaryService.uploadRawFile(backupFile, "backups");
                    fileInfo.setDownloadUrl(uploadResult.getUrl());
                    log.info("Backup successfully uploaded to Cloudinary: {}", uploadResult.getUrl());

                    // Safely clean up local temporary file after cloud upload
                    if (backupFile.exists()) {
                        backupFile.delete();
                    }
                } catch (Exception e) {
                    log.warn("Failed to upload backup to Cloudinary via CloudinaryService. Keeping local copy.", e);
                }
            }

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
        Map<String, BackupFileInfo> fileMap = new HashMap<>();

        // 1. Fetch Cloudinary Backups via CloudinaryService
        if (isCloudinaryAvailable()) {
            try {
                List<Map<String, Object>> resources = cloudinaryService.listResourcesByPrefix("backups/", "raw");
                for (Map<String, Object> res : resources) {
                    String publicId = (String) res.get("public_id");
                    String name = publicId.contains("/") ? publicId.substring(publicId.lastIndexOf('/') + 1) : publicId;
                    long bytes = ((Number) res.get("bytes")).longValue();
                    String createdAtStr = (String) res.get("created_at");
                    String secureUrl = (String) res.get("secure_url");

                    LocalDateTime createdAt = LocalDateTime.now();
                    if (createdAtStr != null) {
                        try {
                            createdAt = ZonedDateTime.parse(createdAtStr).toLocalDateTime();
                        } catch (Exception ignored) {
                        }
                    }

                    BackupFileInfo info = BackupFileInfo.builder()
                            .fileName(name)
                            .sizeBytes(bytes)
                            .formattedSize(formatFileSize(bytes))
                            .createdAt(createdAt)
                            .downloadUrl(secureUrl)
                            .build();

                    fileMap.put(name, info);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch backups list from Cloudinary via CloudinaryService", e);
            }
        }

        // 2. Fetch Local Backups
        File folder = Paths.get(backupDirectoryPath).toFile();
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".sql") || name.endsWith(".dump"));
            if (files != null) {
                for (File file : files) {
                    if (!fileMap.containsKey(file.getName())) {
                        fileMap.put(file.getName(), buildBackupFileInfo(file));
                    }
                }
            }
        }

        List<BackupFileInfo> list = new ArrayList<>(fileMap.values());
        list.sort(Comparator.comparing(BackupFileInfo::getCreatedAt).reversed());
        return list;
    }

    public Resource getBackupFileResource(String fileName) {
        validateFileName(fileName);
        Path filePath = Paths.get(backupDirectoryPath).resolve(fileName).normalize();
        File file = filePath.toFile();

        // 1. Return local file if present
        if (file.exists() && file.isFile()) {
            return new FileSystemResource(file);
        }

        // 2. Fetch from Cloudinary via CloudinaryService if configured
        if (isCloudinaryAvailable()) {
            try {
                String cloudUrl = cloudinaryService.getResourceUrl("backups/" + fileName, "raw");
                if (cloudUrl != null) {
                    try (InputStream in = URI.create(cloudUrl).toURL().openStream()) {
                        byte[] bytes = in.readAllBytes();
                        return new ByteArrayResource(bytes) {
                            @Override
                            public String getFilename() {
                                return fileName;
                            }
                        };
                    }
                }
            } catch (Exception e) {
                log.error("Failed to download backup file from Cloudinary via CloudinaryService: {}", fileName, e);
            }
        }

        throw new IllegalArgumentException("Backup file not found: " + fileName);
    }

    public void deleteBackup(String fileName) {
        validateFileName(fileName);
        boolean deletedLocal = false;
        boolean deletedCloud = false;

        Path filePath = Paths.get(backupDirectoryPath).resolve(fileName).normalize();
        File file = filePath.toFile();
        if (file.exists()) {
            deletedLocal = file.delete();
        }

        if (isCloudinaryAvailable()) {
            try {
                deletedCloud = cloudinaryService.deleteRawFile("backups/" + fileName);
                log.info("Cloudinary delete result for {}: {}", fileName, deletedCloud);
            } catch (Exception e) {
                log.warn("Failed to delete backup from Cloudinary via CloudinaryService: {}", fileName, e);
            }
        }

        if (!deletedLocal && !deletedCloud) {
            throw new IllegalArgumentException("Backup file not found or could not be deleted: " + fileName);
        }
    }

    public BackupResponse restoreBackup(String fileName) {
        validateFileName(fileName);
        File tempRestoreFile = null;

        try {
            Resource resource = getBackupFileResource(fileName);
            if (resource instanceof FileSystemResource fsr) {
                tempRestoreFile = fsr.getFile();
            } else {
                tempRestoreFile = Files.createTempFile("restore_", "_" + fileName).toFile();
                try (InputStream in = resource.getInputStream()) {
                    Files.copy(in, tempRestoreFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }

            DbConnDetails connDetails = parseJdbcUrl(datasourceUrl);

            ProcessBuilder pb;
            if (fileName.endsWith(".sql")) {
                pb = new ProcessBuilder(
                        psqlExecutable,
                        "-h", connDetails.host,
                        "-p", String.valueOf(connDetails.port),
                        "-U", dbUsername,
                        "-d", connDetails.databaseName,
                        "-f", tempRestoreFile.getAbsolutePath());
            } else {
                pb = new ProcessBuilder(
                        pgRestoreExecutable,
                        "-h", connDetails.host,
                        "-p", String.valueOf(connDetails.port),
                        "-U", dbUsername,
                        "-d", connDetails.databaseName,
                        "--clean",
                        "--if-exists",
                        "--no-owner",
                        "--no-acl",
                        tempRestoreFile.getAbsolutePath());
            }

            if (dbPassword != null && !dbPassword.isBlank()) {
                pb.environment().put("PGPASSWORD", dbPassword);
            }

            Process process = pb.start();
            String errorMsg = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("Restore process failed with exit code {}: {}", exitCode, errorMsg);
                return BackupResponse.builder()
                        .success(false)
                        .message("Restore failed: " + (errorMsg.isBlank() ? "Exit code " + exitCode : errorMsg))
                        .build();
            }

            log.info("Database successfully restored from file: {}", fileName);
            return BackupResponse.builder()
                    .success(true)
                    .message("Database restored successfully")
                    .build();

        } catch (IOException e) {
            log.error("IO Exception during restore execution", e);
            return BackupResponse.builder()
                    .success(false)
                    .message("Failed to execute restore command: " + e.getMessage())
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Restore process interrupted", e);
            return BackupResponse.builder()
                    .success(false)
                    .message("Restore process was interrupted.")
                    .build();
        } finally {
            if (tempRestoreFile != null && tempRestoreFile.getName().startsWith("restore_")) {
                tempRestoreFile.delete();
            }
        }
    }

    private boolean isCloudinaryAvailable() {
        return cloudinaryService != null && cloudinaryService.isConfigured();
    }

    private void validateFileName(String fileName) {
        if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Invalid file name provided.");
        }
    }

    private BackupFileInfo buildBackupFileInfo(File file) {
        long bytes = file.length();
        LocalDateTime createdAt;

        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            createdAt = LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            createdAt = LocalDateTime.ofInstant(
                    file.lastModified() > 0 ? java.time.Instant.ofEpochMilli(file.lastModified())
                            : java.time.Instant.now(),
                    ZoneId.systemDefault());
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
