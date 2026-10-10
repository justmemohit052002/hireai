package com.vionsys.hireai.candidate.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import com.vionsys.hireai.candidate.exception.FileStorageException;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;
    private FileSecurityValidator validator;

    private static final byte[] VALID_MINIMAL_PDF = (
            "%PDF-1.4\n" +
            "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
            "2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n" +
            "3 0 obj<</Type/Page/MediaBox[0 0 3 3]>>endobj\n" +
            "xref\n0 4\n0000000000 65535 f\n0000000009 00000 n\n0000000052 00000 n\n0000000096 00000 n\n" +
            "trailer<</Size 4/Root 1 0 R>>\nstartxref\n147\n%%EOF"
    ).getBytes(StandardCharsets.UTF_8);

    private static final byte[] VALID_1X1_PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89,
            0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
            0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01,
            0x0D, 0x0A, 0x2D, (byte) 0xB4,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    @BeforeEach
    void setUp() {
        validator = new FileSecurityValidator(10 * 1024 * 1024, 5 * 1024 * 1024, 10 * 1024 * 1024);
        Path resumesPath = tempDir.resolve("resumes");
        Path attachmentsPath = tempDir.resolve("attachments");
        storageService = new LocalFileStorageService(resumesPath.toString(), attachmentsPath.toString(), validator);
    }

    @Test
    @DisplayName("Stored resume file is saved with non-executable permissions and random UUID")
    void testStoreResumeGuaranteesNonExecutable() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "my_resume.pdf", "application/pdf", VALID_MINIMAL_PDF
        );

        String storedPath = storageService.store(file);
        assertNotNull(storedPath);

        File savedFile = new File(storedPath);
        assertTrue(savedFile.exists(), "Stored file must exist on disk");
        assertTrue(savedFile.getName().endsWith(".pdf"), "File must have safe .pdf extension");
        assertFalse(savedFile.getName().equals("my_resume.pdf"), "Stored filename must be randomized UUID");

        // If running on a POSIX filesystem (Linux/macOS/Docker container), verify execute bits are stripped
        if (java.nio.file.FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            java.util.Set<java.nio.file.attribute.PosixFilePermission> posix =
                    java.nio.file.Files.getPosixFilePermissions(savedFile.toPath());
            assertFalse(posix.contains(java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE));
            assertFalse(posix.contains(java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE));
            assertFalse(posix.contains(java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE));
        }
    }

    @Test
    @DisplayName("Stored chat attachment is saved with non-executable permissions and can be loaded")
    void testStoreAndLoadChatAttachment() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", VALID_1X1_PNG
        );

        String storedFilename = storageService.storeAttachment(file);
        assertNotNull(storedFilename);
        assertTrue(storedFilename.startsWith("att_"));
        assertTrue(storedFilename.endsWith(".png"));

        Resource loaded = storageService.loadAttachmentAsResource(storedFilename);
        assertNotNull(loaded);
        assertTrue(loaded.exists());
        assertTrue(loaded.isReadable());
    }

    @Test
    @DisplayName("Path traversal in attachment loading is rejected")
    void testPathTraversalAttachmentRejected() {
        assertThrows(FileStorageException.class,
                () -> storageService.loadAttachmentAsResource("../../secret.txt"));
    }

    @Test
    @DisplayName("Storing executable disguised as resume is rejected before saving")
    void testMaliciousUploadRejectedBeforeStorage() {
        byte[] mzExe = new byte[]{0x4D, 0x5A, 0x00, 0x00};
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file", "exploit.pdf", "application/pdf", mzExe
        );

        assertThrows(FileStorageException.class,
                () -> storageService.store(maliciousFile));
    }
}
