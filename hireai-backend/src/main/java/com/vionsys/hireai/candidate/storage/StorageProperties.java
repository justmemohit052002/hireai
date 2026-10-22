package com.vionsys.hireai.candidate.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Single source of truth for all file storage and upload directories across HireAI.
 * Resolves app.upload.dir to an absolute, normalized path that remains stable
 * regardless of the working directory from which the application was started.
 */
@Slf4j
@Getter
@Component
public class StorageProperties {

    private final Path baseUploadPath;
    private final Path resumeUploadPath;
    private final Path attachmentUploadPath;
    private final Path photoUploadPath;

    public StorageProperties(
            @Value("${app.upload.dir:${HIREAI_UPLOAD_DIR:${APP_UPLOAD_DIR:}}}") String configuredBaseDir,
            @Value("${file.upload-dir:}") String configuredResumeDir,
            @Value("${file.attachment-dir:}") String configuredAttachmentDir,
            @Value("${photo.upload-dir:}") String configuredPhotoDir) {

        this.baseUploadPath = resolveBaseDirectory(configuredBaseDir);

        this.resumeUploadPath = (configuredResumeDir != null && !configuredResumeDir.isBlank() && Paths.get(configuredResumeDir).isAbsolute())
                ? Paths.get(configuredResumeDir).normalize()
                : (configuredResumeDir != null && !configuredResumeDir.isBlank() && !configuredResumeDir.equals("uploads/resumes") && !configuredResumeDir.startsWith("uploads/"))
                    ? this.baseUploadPath.resolve(configuredResumeDir).normalize()
                    : this.baseUploadPath.resolve("resumes").normalize();

        this.attachmentUploadPath = (configuredAttachmentDir != null && !configuredAttachmentDir.isBlank() && Paths.get(configuredAttachmentDir).isAbsolute())
                ? Paths.get(configuredAttachmentDir).normalize()
                : (configuredAttachmentDir != null && !configuredAttachmentDir.isBlank() && !configuredAttachmentDir.equals("uploads/attachments") && !configuredAttachmentDir.startsWith("uploads/"))
                    ? this.baseUploadPath.resolve(configuredAttachmentDir).normalize()
                    : this.baseUploadPath.resolve("attachments").normalize();

        this.photoUploadPath = (configuredPhotoDir != null && !configuredPhotoDir.isBlank() && Paths.get(configuredPhotoDir).isAbsolute())
                ? Paths.get(configuredPhotoDir).normalize()
                : (configuredPhotoDir != null && !configuredPhotoDir.isBlank() && !configuredPhotoDir.equals("uploads/profile-photos") && !configuredPhotoDir.startsWith("uploads/"))
                    ? this.baseUploadPath.resolve(configuredPhotoDir).normalize()
                    : this.baseUploadPath.resolve("profile-photos").normalize();
    }

    /**
     * Resolves the storage base directory to an explicit, stable absolute path.
     * When unconfigured, prevents drift between process starts from repository root vs module directory.
     */
    public static Path resolveBaseDirectory(String configuredBaseDir) {
        return resolveBaseDirectory(configuredBaseDir, null);
    }

    /**
     * Resolves storage base directory with an optional simulated working directory.
     * Guaranteed to return identical absolute paths when an absolute directory or environment variable is set.
     */
    public static Path resolveBaseDirectory(String configuredBaseDir, Path workingDir) {
        String envHireAiDir = System.getenv("HIREAI_UPLOAD_DIR");
        String envAppDir = System.getenv("APP_UPLOAD_DIR");

        String explicitDir = (configuredBaseDir != null && !configuredBaseDir.isBlank())
                ? configuredBaseDir.trim()
                : (envHireAiDir != null && !envHireAiDir.isBlank())
                    ? envHireAiDir.trim()
                    : (envAppDir != null && !envAppDir.isBlank())
                        ? envAppDir.trim()
                        : null;

        if (explicitDir != null) {
            Path custom = Paths.get(explicitDir);
            if (custom.isAbsolute()) {
                return custom.normalize();
            }
            Path anchor = (workingDir != null) ? workingDir : getCanonicalRepoRoot();
            return anchor.resolve(custom).normalize();
        }

        // Default: Canonical repo root / uploads (independent of launch working directory)
        return getCanonicalRepoRoot().resolve("uploads").normalize();
    }

    /**
     * Determines the canonical repository root deterministically across launch environments.
     */
    public static Path getCanonicalRepoRoot() {
        try {
            var codeSource = StorageProperties.class.getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                Path codePath = Paths.get(codeSource.getLocation().toURI()).toAbsolutePath().normalize();
                Path cursor = codePath;
                while (cursor != null) {
                    if (Files.isDirectory(cursor.resolve("hireai-backend")) && Files.isDirectory(cursor.resolve("uploads"))) {
                        return cursor;
                    }
                    if (Files.isDirectory(cursor.resolve("hireai-backend")) && Files.isDirectory(cursor.resolve("hireai-frontend"))) {
                        return cursor;
                    }
                    if (cursor.getFileName() != null && cursor.getFileName().toString().equals("hireai-backend")) {
                        Path parent = cursor.getParent();
                        if (parent != null) {
                            return parent;
                        }
                    }
                    cursor = cursor.getParent();
                }
            }
        } catch (Exception ignored) {
        }

        Path cwd = Paths.get("").toAbsolutePath().normalize();
        if (Files.isDirectory(cwd.resolve("hireai-backend"))) {
            return cwd;
        }
        Path parent = cwd.getParent();
        if (parent != null && Files.isDirectory(parent.resolve("hireai-backend"))) {
            return parent;
        }

        return cwd;
    }

    @PostConstruct
    public void initAndVerify() {
        try {
            Files.createDirectories(this.baseUploadPath);
            Files.createDirectories(this.resumeUploadPath);
            Files.createDirectories(this.attachmentUploadPath);
            Files.createDirectories(this.photoUploadPath);

            // Fail-fast write probe test
            Path probe = this.baseUploadPath.resolve(".write_probe_" + UUID.randomUUID());
            Files.writeString(probe, "write-probe");
            Files.deleteIfExists(probe);

            log.info("Initialized unified storage base directory: {}", this.baseUploadPath);
            log.info("Storage subdirectories: Resumes: {}, Attachments: {}, Profile Photos: {}",
                    this.resumeUploadPath, this.attachmentUploadPath, this.photoUploadPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Storage directory cannot be created or is not writable: "
                    + this.baseUploadPath + " (" + ex.getMessage() + ")", ex);
        }
    }
}
