package com.rentsphere.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.rentsphere.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public record UploadResult(String url, String publicId) {}

    public UploadResult uploadImage(MultipartFile file, String folder) {
        validateFile(file, "image");
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "folder",          "rentsphere/" + folder,
                    "resource_type",   "image",
                    "allowed_formats", "jpg,jpeg,png,webp",
                    "transformation",  "q_auto,f_auto"
                )
            );
            return new UploadResult(
                (String) result.get("secure_url"),
                (String) result.get("public_id")
            );
        } catch (IOException e) {
            log.error("Cloudinary image upload failed: {}", e.getMessage());
            throw new BusinessException("Failed to upload image. Please try again.");
        }
    }

    public UploadResult uploadDocument(MultipartFile file, String folder) {
        validateFile(file, "document");
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "folder",        "rentsphere/" + folder,
                    "resource_type", "raw",
                    "allowed_formats","pdf,doc,docx"
                )
            );
            return new UploadResult(
                (String) result.get("secure_url"),
                (String) result.get("public_id")
            );
        } catch (IOException e) {
            log.error("Cloudinary document upload failed: {}", e.getMessage());
            throw new BusinessException("Failed to upload document. Please try again.");
        }
    }

    public void deleteFile(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap("resource_type", resourceType)
            );
            log.info("Deleted Cloudinary asset: {}", publicId);
        } catch (IOException e) {
            log.warn("Failed to delete Cloudinary asset {}: {}", publicId, e.getMessage());
        }
    }

    public void deleteImage(String publicId) {
        deleteFile(publicId, "image");
    }

    public void deleteDocument(String publicId) {
        deleteFile(publicId, "raw");
    }

    private void validateFile(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Cannot upload empty " + type + ".");
        }
        long maxBytes = "image".equals(type) ? 5 * 1024 * 1024L : 10 * 1024 * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("File exceeds maximum allowed size.");
        }
    }
}
