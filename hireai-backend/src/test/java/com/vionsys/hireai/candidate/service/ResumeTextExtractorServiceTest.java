package com.vionsys.hireai.candidate.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ResumeTextExtractorServiceTest {

    private ResumeTextExtractorService extractorService;

    @BeforeEach
    void setUp() {
        extractorService = new ResumeTextExtractorService();
    }

    @Test
    void testExtractTextFromPlainTextFile() {
        String content = "John Doe\nSenior Backend Engineer\nSkills: Java, Spring Boot, PostgreSQL, Docker";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.txt",
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );

        String extracted = extractorService.extractText(file);
        assertNotNull(extracted);
        assertTrue(extracted.contains("John Doe"));
        assertTrue(extracted.contains("Spring Boot"));
    }

    @Test
    void testExtractTextFromEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        String extracted = extractorService.extractText(emptyFile);
        assertNotNull(extracted);
        assertTrue(extracted.isEmpty());
    }
}
