package com.vionsys.hireai.candidate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.exception.FileStorageException;

@ExtendWith(MockitoExtension.class)
class ResumeTextExtractorServiceUnitTest {

    private ResumeTextExtractorService extractorService;

    @BeforeEach
    void setUp() {
        extractorService = new ResumeTextExtractorService();
    }

    @Test
    @DisplayName("shouldReturnEmptyString_whenFileIsNull")
    void shouldReturnEmptyString_whenFileIsNull() {
        String result = extractorService.extractText((MultipartFile) null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("shouldReturnEmptyString_whenFileIsEmpty")
    void shouldReturnEmptyString_whenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("resume", "resume.pdf", "application/pdf", new byte[0]);
        String result = extractorService.extractText(emptyFile);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("shouldExtractTextFromPlainTextFile_andNormalizeWhitespace")
    void shouldExtractTextFromPlainTextFile_andNormalizeWhitespace() {
        String rawContent = "John Doe\r\nSoftware Engineer\r\n\r\n\r\nSkills: Java, Spring Boot";
        MockMultipartFile textFile = new MockMultipartFile(
                "resume",
                "resume.txt",
                "text/plain",
                rawContent.getBytes(StandardCharsets.UTF_8)
        );

        String extracted = extractorService.extractText(textFile);

        assertThat(extracted).contains("John Doe");
        assertThat(extracted).contains("Software Engineer");
        assertThat(extracted).contains("Skills: Java, Spring Boot");
        // Verify whitespace normalization
        assertThat(extracted).doesNotContain("\r\n");
    }

    @Test
    @DisplayName("shouldThrowFileStorageException_whenFileStreamFailsOrIsCorrupted")
    void shouldThrowFileStorageException_whenFileStreamFailsOrIsCorrupted() throws IOException {
        MultipartFile faultyFile = mock(MultipartFile.class);
        when(faultyFile.isEmpty()).thenReturn(false);
        when(faultyFile.getOriginalFilename()).thenReturn("corrupted.pdf");
        when(faultyFile.getInputStream()).thenThrow(new IOException("Stream reading failed"));

        assertThrows(FileStorageException.class, () -> extractorService.extractText(faultyFile));
    }

    @Test
    @DisplayName("shouldReturnEmptyString_whenFilePathIsNull")
    void shouldReturnEmptyString_whenFilePathIsNull() {
        String result = extractorService.extractTextFromPath(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("shouldReturnEmptyString_whenFilePathDoesNotExist")
    void shouldReturnEmptyString_whenFilePathDoesNotExist() {
        String result = extractorService.extractTextFromPath("non_existent_directory/resume.pdf");
        assertThat(result).isEmpty();
    }
}

