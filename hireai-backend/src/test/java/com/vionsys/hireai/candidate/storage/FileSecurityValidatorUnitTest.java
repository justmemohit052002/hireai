package com.vionsys.hireai.candidate.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.vionsys.hireai.candidate.exception.FileStorageException;

class FileSecurityValidatorUnitTest {

    private FileSecurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FileSecurityValidator(
                10485760L, // 10MB max resume
                5242880L,  // 5MB max photo
                10485760L  // 10MB max attachment
        );
    }

    @Test
    @DisplayName("shouldAcceptValidPdfFile_whenHeadersAndMimeMatch")
    void shouldAcceptValidPdfFile_whenHeadersAndMimeMatch() {
        // Valid PDF magic bytes: %PDF-1.4 ...
        byte[] validPdfBytes = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        MockMultipartFile pdfFile = new MockMultipartFile(
                "resume",
                "valid_resume.pdf",
                "application/pdf",
                validPdfBytes
        );

        assertDoesNotThrow(() -> validator.validateResumeFile(pdfFile));
    }

    @Test
    @DisplayName("shouldRejectFile_whenFileIsNull")
    void shouldRejectFile_whenFileIsNull() {
        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateResumeFile(null));
        assertThat(ex.getMessage()).containsIgnoringCase("empty");
    }

    @Test
    @DisplayName("shouldRejectFile_whenFileIsEmpty")
    void shouldRejectFile_whenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("resume", "empty.pdf", "application/pdf", new byte[0]);
        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateResumeFile(emptyFile));
        assertThat(ex.getMessage()).containsIgnoringCase("empty");
    }

    @Test
    @DisplayName("shouldRejectUnsupportedFileExtension_whenExtensionIsNotDocument")
    void shouldRejectUnsupportedFileExtension_whenExtensionIsNotDocument() {
        MockMultipartFile imageFile = new MockMultipartFile(
                "resume",
                "photo.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateResumeFile(imageFile));
        assertThat(ex.getMessage()).contains("Unsupported");
    }

    @Test
    @DisplayName("shouldRejectDangerousExecutableFiles_suchAsExeOrScript")
    void shouldRejectDangerousExecutableFiles_suchAsExeOrScript() {
        MockMultipartFile scriptFile = new MockMultipartFile(
                "resume",
                "malicious.sh",
                "text/x-sh",
                "#!/bin/bash\nrm -rf /".getBytes(StandardCharsets.UTF_8)
        );

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateResumeFile(scriptFile));
        assertThat(ex.getMessage()).containsIgnoringCase("unsupported file format");
    }

    @Test
    @DisplayName("shouldRejectCorruptedOrSpoofedFile_whenMagicBytesDoNotMatchExtension")
    void shouldRejectCorruptedOrSpoofedFile_whenMagicBytesDoNotMatchExtension() {
        // Filename says .pdf, but content is random plaintext without %PDF header
        byte[] fakePdfBytes = "Plain text disguised as a PDF document".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile spoofedFile = new MockMultipartFile(
                "resume",
                "spoofed.pdf",
                "application/pdf",
                fakePdfBytes
        );

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> validator.validateResumeFile(spoofedFile));
        assertThat(ex.getMessage()).contains("does not match a valid");
    }
}
