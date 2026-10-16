package com.vionsys.hireai.candidate.storage;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.exception.FileStorageException;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for securely storing, serving, and deleting candidate and recruiter profile photos.
 * Ensures:
 * - Content, extension, and magic-byte validation (blocking SVG/scripts/executables)
 * - Isolated storage location outside web root
 * - Cryptographically random UUID filenames with sanitized extensions
 * - Strict non-executable file permissions on creation
 * - Path traversal defense
 */
@Slf4j
@Service
public class ProfilePhotoStorageService {

    private final Path photoUploadPath;
    private final FileSecurityValidator fileSecurityValidator;

    public ProfilePhotoStorageService(
            @Value("${photo.upload-dir:uploads/profile-photos}") String photoUploadDir,
            FileSecurityValidator fileSecurityValidator) {
        this.fileSecurityValidator = fileSecurityValidator;
        this.photoUploadPath = Paths.get(photoUploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.photoUploadPath);
            log.info("Initialized secure profile photo storage directory at: {}", this.photoUploadPath);
        } catch (IOException ex) {
            throw new FileStorageException("Could not initialize photo upload directory: " + this.photoUploadPath, ex);
        }
    }

    public String storePhoto(MultipartFile file) {
        // 1. Deep security validation (magic bytes, MIME type, size, ImageIO decode, no embedded scripts)
        fileSecurityValidator.validateProfilePhoto(file);

        String originalFileName = file.getOriginalFilename();
        String extension = fileSecurityValidator.getSanitizedExtension(
                originalFileName, FileSecurityValidator.ALLOWED_IMAGE_EXTENSIONS);

        // 2. Generate cryptographically random filename with safe extension
        String storedFileName = "photo_" + UUID.randomUUID() + extension;
        Path targetLocation = this.photoUploadPath.resolve(storedFileName).normalize();

        if (!targetLocation.startsWith(this.photoUploadPath)) {
            throw new FileStorageException("Security violation: path traversal attempt during photo storage.");
        }

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 3. Guarantee photo cannot be executed as code on disk
            enforceNonExecutablePermissions(targetLocation);

            log.info("Saved validated profile photo {} -> {}", originalFileName, targetLocation);
            return targetLocation.toString();
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store profile photo: " + ex.getMessage(), ex);
        }
    }

    public Resource loadPhotoAsResource(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new FileStorageException("Profile photo path is missing.");
        }

        // Check for directory traversal sequences
        if (filePath.contains("..") || filePath.contains("\0")) {
            throw new FileStorageException("Unauthorized photo access path.");
        }

        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            if (!path.startsWith(this.photoUploadPath)) {
                // If relative filename passed, resolve against photoUploadPath
                path = this.photoUploadPath.resolve(Paths.get(filePath).getFileName()).normalize();
            }

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
            if (!path.startsWith(this.photoUploadPath)) {
                path = this.photoUploadPath.resolve(Paths.get(filePath).getFileName()).normalize();
            }

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

    private void enforceNonExecutablePermissions(Path path) {
        try {
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                Set<PosixFilePermission> permissions = EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.GROUP_READ
                );
                Files.setPosixFilePermissions(path, permissions);
            } else {
                File file = path.toFile();
                file.setReadable(true, false);
                file.setWritable(true, true);
                file.setExecutable(false, false);
            }
        } catch (Exception ex) {
            log.warn("Could not enforce strict non-executable permissions on {}: {}", path, ex.getMessage());
        }
    }
}
