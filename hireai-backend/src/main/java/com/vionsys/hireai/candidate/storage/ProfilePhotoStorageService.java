package com.vionsys.hireai.candidate.storage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.exception.FileStorageException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProfilePhotoStorageService {

    private final Path photoUploadPath;
    private final long maxPhotoSize;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );

    private static final List<String> ALLOWED_IMAGE_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    public ProfilePhotoStorageService(
            @Value("${photo.upload-dir:uploads/profile-photos}") String photoUploadDir,
            @Value("${photo.max-file-size:5242880}") long maxPhotoSize) {
        this.photoUploadPath = Paths.get(photoUploadDir).toAbsolutePath().normalize();
        this.maxPhotoSize = maxPhotoSize;

        try {
            Files.createDirectories(this.photoUploadPath);
            log.info("Initialized profile photo storage directory at: {}", this.photoUploadPath);
        } catch (IOException ex) {
            throw new FileStorageException("Could not initialize photo upload directory: " + this.photoUploadPath, ex);
        }
    }

    public String storePhoto(MultipartFile file) {
        validateImageFile(file);

        String originalFileName = file.getOriginalFilename();
        String extension = "";
        int extIndex = originalFileName != null ? originalFileName.lastIndexOf(".") : -1;
        if (extIndex > 0) {
            extension = originalFileName.substring(extIndex).toLowerCase();
        }

        String storedFileName = "photo_" + UUID.randomUUID() + extension;
        Path targetLocation = this.photoUploadPath.resolve(storedFileName).normalize();

        if (!targetLocation.getParent().equals(this.photoUploadPath)) {
            throw new FileStorageException("Security violation: path traversal attempt.");
        }

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved profile photo {} -> {}", originalFileName, targetLocation);
            return targetLocation.toString();
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store profile photo: " + ex.getMessage(), ex);
        }
    }

    public Resource loadPhotoAsResource(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new FileStorageException("Profile photo path is missing.");
        }

        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            if (!path.startsWith(this.photoUploadPath)) {
                throw new FileStorageException("Unauthorized photo access path.");
            }

            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageException("Profile photo file could not be found or is unreadable.");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new FileStorageException("Invalid photo URL: " + ex.getMessage(), ex);
        }
    }

    public void deletePhoto(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            if (path.startsWith(this.photoUploadPath)) {
                Files.deleteIfExists(path);
                log.info("Deleted profile photo at: {}", path);
            }
        } catch (IOException ex) {
            log.warn("Failed to delete profile photo at {}: {}", filePath, ex.getMessage());
        }
    }

    public MediaType determineMediaType(String filePath) {
        if (filePath == null) {
            return MediaType.IMAGE_JPEG;
        }
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        } else if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        return MediaType.IMAGE_JPEG;
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile photo file cannot be empty.");
        }

        if (file.getSize() > maxPhotoSize) {
            throw new IllegalArgumentException(String.format("Profile photo size exceeds maximum limit of %d MB.", maxPhotoSize / (1024 * 1024)));
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("File name is missing.");
        }

        String ext = "";
        int extIndex = originalName.lastIndexOf(".");
        if (extIndex > 0) {
            ext = originalName.substring(extIndex).toLowerCase();
        }

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Unsupported image format: " + ext + ". Allowed formats: JPG, JPEG, PNG, WEBP, GIF.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !ALLOWED_IMAGE_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid image content type: " + contentType);
        }
    }
}
