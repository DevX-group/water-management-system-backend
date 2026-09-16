package com.backend.water_management_system.payments.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.backend.water_management_system.payments.dto.CloudinaryUploadResponse;
import com.backend.water_management_system.payments.exceptions.CloudinaryDeleteException;
import com.backend.water_management_system.payments.exceptions.CloudinaryUploadException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;
    public boolean isConfigured() {
        return true;
    }
    @SuppressWarnings("unchecked")
    public CloudinaryUploadResponse uploadFile(MultipartFile file) {
        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("resource_type", "auto"));

            return CloudinaryUploadResponse.builder()
                    .url(uploadResult.get("secure_url").toString())
                    .publicId(uploadResult.get("public_id").toString())
                    .build();

        } catch (IOException e) {
            throw new CloudinaryUploadException("Upload failed", e);
        }
    }
    public CloudinaryUploadResponse uploadRawFile(File file, String folder) {
        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "raw",
                    "use_filename", true,
                    "unique_filename", false);

            Map<String, Object> uploadResult = cloudinary.uploader().upload(file, params);

            return CloudinaryUploadResponse.builder()
                    .url(uploadResult.get("secure_url").toString())
                    .publicId(uploadResult.get("public_id").toString())
                    .build();

        } catch (IOException e) {
            throw new CloudinaryUploadException("Raw file upload failed: " + e.getMessage(), e);
        }
    }
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listResourcesByPrefix(String prefix, String resourceType) {
        try {
            Map<?, ?> response = cloudinary.api().resources(ObjectUtils.asMap(
                    "type", "upload",
                    "prefix", prefix,
                    "resource_type", resourceType,
                    "max_results", 100));

            List<?> resources = (List<?>) response.get("resources");
            if (resources == null) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Object obj : resources) {
                if (obj instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to list Cloudinary resources with prefix '{}': {}", prefix, e.getMessage());
            return Collections.emptyList();
        }
    }

    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new CloudinaryDeleteException("Deletion failed", e);
        }
    }
    public boolean deleteRawFile(String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
            String status = (String) result.get("result");
            return "ok".equals(status);
        } catch (Exception e) {
            log.warn("Failed to delete raw Cloudinary file '{}': {}", publicId, e.getMessage());
            return false;
        }
    }
    public String getResourceUrl(String publicId, String resourceType) {
        try {
            Map<?, ?> res = cloudinary.api().resource(publicId, ObjectUtils.asMap("resource_type", resourceType));
            return (String) res.get("secure_url");
        } catch (Exception e) {
            return null;
        }
    }
}
