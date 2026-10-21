package com.vionsys.hireai.candidate.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vionsys.hireai.candidate.exception.FileStorageException;

class StoragePropertiesTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Explicit absolute path configured via app.upload.dir is respected")
    void testExplicitAbsolutePathRespected() {
        Path customBase = tempDir.resolve("custom_uploads").toAbsolutePath().normalize();
        StorageProperties properties = new StorageProperties(
                customBase.toString(), "", "", ""
        );

        assertEquals(customBase, properties.getBaseUploadPath());
        assertEquals(customBase.resolve("resumes"), properties.getResumeUploadPath());
        assertEquals(customBase.resolve("attachments"), properties.getAttachmentUploadPath());
        assertEquals(customBase.resolve("profile-photos"), properties.getPhotoUploadPath());
    }

    @Test
    @DisplayName("Default base directory resolution anchors consistently to absolute project uploads folder")
    void testDefaultBaseDirectoryResolution() {
        Path resolved = StorageProperties.resolveBaseDirectory("");
        assertNotNull(resolved);
        assertTrue(resolved.isAbsolute(), "Resolved upload path must be an absolute path");
        assertTrue(resolved.endsWith("uploads"), "Resolved base directory must end with 'uploads'");
    }

    @Test
    @DisplayName("Two different working directories resolve to the same path when the setting is absolute")
    void testTwoDifferentWorkingDirectoriesResolveToSamePathWhenAbsolute() {
        Path absoluteTarget = tempDir.resolve("enterprise_storage").toAbsolutePath().normalize();
        Path workingDirA = tempDir.resolve("simulated_repo_root");
        Path workingDirB = tempDir.resolve("simulated_module_dir/hireai-backend");

        Path resolvedA = StorageProperties.resolveBaseDirectory(absoluteTarget.toString(), workingDirA);
        Path resolvedB = StorageProperties.resolveBaseDirectory(absoluteTarget.toString(), workingDirB);

        assertEquals(absoluteTarget, resolvedA);
        assertEquals(absoluteTarget, resolvedB);
        assertEquals(resolvedA, resolvedB, "Both working directories must resolve to the identical absolute path");
    }

    @Test
    @DisplayName("Storage services share the identical base and subdirectories from StorageProperties")
    void testServicesShareSameBaseDirectory() {
        Path customBase = tempDir.resolve("shared_uploads").toAbsolutePath().normalize();
        StorageProperties properties = new StorageProperties(
                customBase.toString(), "", "", ""
        );
        properties.initAndVerify();

        FileSecurityValidator validator = new FileSecurityValidator(10 * 1024 * 1024, 5 * 1024 * 1024, 10 * 1024 * 1024);
        LocalFileStorageService fileStorageService = new LocalFileStorageService(properties, validator);
        ProfilePhotoStorageService photoStorageService = new ProfilePhotoStorageService(properties, validator);

        assertEquals(properties.getResumeUploadPath(), customBase.resolve("resumes"));
        assertEquals(properties.getPhotoUploadPath(), customBase.resolve("profile-photos"));
        assertEquals(properties.getAttachmentUploadPath(), customBase.resolve("attachments"));

        assertTrue(Files.exists(properties.getResumeUploadPath()), "Resume dir must be created");
        assertTrue(Files.exists(properties.getPhotoUploadPath()), "Photo dir must be created");
        assertTrue(Files.exists(properties.getAttachmentUploadPath()), "Attachment dir must be created");
    }

    @Test
    @DisplayName("Path traversal attempts in attachment and resume loading are rejected")
    void testPathTraversalRejected() {
        Path customBase = tempDir.resolve("secure_uploads").toAbsolutePath().normalize();
        StorageProperties properties = new StorageProperties(
                customBase.toString(), "", "", ""
        );
        properties.initAndVerify();

        FileSecurityValidator validator = new FileSecurityValidator(10 * 1024 * 1024, 5 * 1024 * 1024, 10 * 1024 * 1024);
        LocalFileStorageService fileStorageService = new LocalFileStorageService(properties, validator);
        ProfilePhotoStorageService photoStorageService = new ProfilePhotoStorageService(properties, validator);

        assertThrows(FileStorageException.class, () ->
                fileStorageService.loadAttachmentAsResource("../../etc/passwd"));
        assertThrows(FileStorageException.class, () ->
                fileStorageService.loadAttachmentAsResource("..\\..\\windows\\system32"));
        assertThrows(FileStorageException.class, () ->
                photoStorageService.loadPhotoAsResource("../../secrets.env"));
    }
}
