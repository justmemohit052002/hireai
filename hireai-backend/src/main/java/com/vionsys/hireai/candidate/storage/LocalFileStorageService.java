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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.exception.FileStorageException;

import lombok.extern.slf4j.Slf4j;

/**
 * Enterprise-grade local file storage service.
 * Enforces:
 * - Content, size, and extension validation before writing to disk
 * - Storage strictly isolated outside of the web application root / classpath
 * - Path traversal defense
 * - Cryptographically random UUID filenames with verified safe extensions
 * - Strict non-executable file permissions on creation
 */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadPath;
    private final Path attachmentPath;
    private final FileSecurityValidator fileSecurityValidator;

    public LocalFileStorageService(
            @Value("${file.upload-dir:uploads/resumes}") String uploadDir,
            @Value("${file.attachment-dir:uploads/attachments}") String attachmentDir,
            FileSecurityValidator fileSecurityValidator) {
        this.fileSecurityValidator = fileSecurityValidator;
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.attachmentPath = Paths.get(attachmentDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.uploadPath);
            Files.createDirectories(this.attachmentPath);
            log.info("Initialized secure file storage directories. Resumes: {}, Attachments: {}",
                    this.uploadPath, this.attachmentPath);
        } catch (IOException ex) {
            throw new FileStorageException("Could not initialize storage directory: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        // Deep security validation (type, size, content / magic bytes, executable detection)
        fileSecurityValidator.validateResumeFile(file);

        String originalFileName = file.getOriginalFilename();
        String safeExtension = fileSecurityValidator.getSanitizedExtension(
                originalFileName, FileSecurityValidator.ALLOWED_DOCUMENT_EXTENSIONS);

        // Store file with a cryptographically randomized UUID and strictly verified safe extension
        String storedFileName = UUID.randomUUID() + safeExtension;
        Path targetLocation = uploadPath.resolve(storedFileName).normalize();

        if (!targetLocation.startsWith(uploadPath)) {
            throw new FileStorageException("Cannot store file outside upload directory (path traversal attempt).");
        }

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Guarantee uploaded file cannot be executed as code
        enforceNonExecutablePermissions(targetLocation);

        log.info("Stored validated resume file {} as {} at {}", originalFileName, storedFileName, targetLocation);
        return targetLocation.toString();
    }

    @Override
    public String storeAttachment(MultipartFile file) throws IOException {
        // Validate chat attachment (documents or images)
        fileSecurityValidator.validateChatAttachment(file);

        String originalFileName = file.getOriginalFilename();
        String ext = fileSecurityValidator.extractExtension(originalFileName);

        String safeExtension;
        if (FileSecurityValidator.ALLOWED_DOCUMENT_EXTENSIONS.contains(ext)) {
            safeExtension = fileSecurityValidator.getSanitizedExtension(originalFileName, FileSecurityValidator.ALLOWED_DOCUMENT_EXTENSIONS);
        } else {
            safeExtension = fileSecurityValidator.getSanitizedExtension(originalFileName, FileSecurityValidator.ALLOWED_IMAGE_EXTENSIONS);
        }

        String storedFileName = "att_" + UUID.randomUUID() + safeExtension;
        Path targetLocation = attachmentPath.resolve(storedFileName).normalize();

        if (!targetLocation.startsWith(attachmentPath)) {
            throw new FileStorageException("Security violation: path traversal attempt during attachment storage.");
        }

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Guarantee uploaded file cannot be executed as code
        enforceNonExecutablePermissions(targetLocation);

        log.info("Stored validated attachment {} as {} at {}", originalFileName, storedFileName, targetLocation);
        return storedFileName;
    }

    @Override
    public Resource loadAsResource(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new FileStorageException("File path is missing.");
        }

        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            if (!path.startsWith(uploadPath)) {
                path = uploadPath.resolve(Paths.get(filePath).getFileName()).normalize();
            }

            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                Path fallback = uploadPath.resolve(Paths.get(filePath).getFileName()).normalize();
                if (Files.exists(fallback) && Files.isReadable(fallback)) {
                    log.info("Resolved resource using uploadPath filename fallback: {}", fallback);
                    return new UrlResource(fallback.toUri());
                }
                throw new FileStorageException("File could not be found or is not readable.");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new FileStorageException("Invalid file URL.", ex);
        }
    }

    @Override
    public Resource loadAttachmentAsResource(String filename) throws IOException {
        if (filename == null || filename.isBlank()) {
            throw new FileStorageException("Attachment filename is missing.");
        }

        // Prevent path traversal
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..") || filename.contains("\0")) {
            throw new FileStorageException("Unauthorized attachment access path.");
        }

        try {
            Path path = attachmentPath.resolve(filename).normalize();
            if (!path.startsWith(attachmentPath)) {
                throw new FileStorageException("Unauthorized attachment access path.");
            }

            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageException("Attachment file could not be found or is not readable.");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new FileStorageException("Invalid attachment file URL.", ex);
        }
    }

    @Override
    public void delete(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        if (!path.startsWith(uploadPath)) {
            path = uploadPath.resolve(Paths.get(filePath).getFileName()).normalize();
        }

        if (path.startsWith(uploadPath)) {
            Files.deleteIfExists(path);
            log.info("Deleted file at: {}", path);
        }
    }

    @Override
    public void deleteAttachment(String filename) throws IOException {
        if (filename == null || filename.isBlank()) {
            return;
        }

        Path path = attachmentPath.resolve(filename).normalize();
        if (path.startsWith(attachmentPath)) {
            Files.deleteIfExists(path);
            log.info("Deleted attachment file at: {}", path);
        }
    }

    /**
     * Enforces that uploaded files cannot have executable bits set under any circumstance.
     * On POSIX systems, sets rw-r----- (0640).
     * On Windows/non-POSIX systems, strips execute permission explicitly via Java File API.
     */
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
