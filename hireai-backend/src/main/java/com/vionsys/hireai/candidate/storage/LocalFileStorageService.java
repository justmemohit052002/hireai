package com.vionsys.hireai.candidate.storage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.exception.FileStorageException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadPath;

    public LocalFileStorageService(@Value("${file.upload-dir:uploads/resumes}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
            log.info("Initialized local file storage directory at: {}", this.uploadPath);
        } catch (IOException ex) {
            throw new FileStorageException("Could not initialize upload directory: " + this.uploadPath, ex);
        }
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Cannot store an empty file.");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new FileStorageException("File name is missing.");
        }

        String extension = "";
        int extensionIndex = originalFileName.lastIndexOf(".");
        if (extensionIndex > 0) {
            extension = originalFileName.substring(extensionIndex);
        }

        String storedFileName = UUID.randomUUID() + extension;
        Path targetLocation = uploadPath.resolve(storedFileName).normalize();

        if (!targetLocation.getParent().equals(uploadPath)) {
            throw new FileStorageException("Cannot store file outside current directory (path traversal attempt).");
        }

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored file {} to {}", originalFileName, targetLocation);
        return targetLocation.toString();
    }

    @Override
    public Resource loadAsResource(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new FileStorageException("Resume file path is missing.");
        }

        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            if (!path.startsWith(uploadPath)) {
                throw new FileStorageException("Unauthorized file path access.");
            }

            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageException("Resume file could not be found or is not readable.");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new FileStorageException("Invalid file URL.", ex);
        }
    }

    @Override
    public void delete(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        if (!path.startsWith(uploadPath)) {
            throw new FileStorageException("Unauthorized file deletion path.");
        }

        Files.deleteIfExists(path);
        log.info("Deleted file at: {}", path);
    }
}
