package com.vionsys.hireai.candidate.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.exception.FileStorageException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ResumeTextExtractorService {

    private final Tika tika = new Tika();

    /**
     * Extracts text content from a MultipartFile (PDF/DOCX/DOC).
     */
    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }

        try (InputStream is = file.getInputStream()) {
            String parsedText = tika.parseToString(is);
            if (parsedText == null) {
                return "";
            }
            return cleanExtractedText(parsedText);
        } catch (Exception ex) {
            log.error("Failed to extract text from uploaded file {}: {}", file.getOriginalFilename(), ex.getMessage());
            throw new FileStorageException("Failed to extract text from resume file: " + ex.getMessage(), ex);
        }
    }

    /**
     * Extracts text content from a stored file path.
     */
    public String extractTextFromPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "";
        }

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "";
            }
            try (InputStream is = Files.newInputStream(path)) {
                String parsedText = tika.parseToString(is);
                return cleanExtractedText(parsedText);
            }
        } catch (Exception ex) {
            log.error("Failed to extract text from path {}: {}", filePath, ex.getMessage());
            return "";
        }
    }

    private String cleanExtractedText(String text) {
        if (text == null) {
            return "";
        }
        // Normalize whitespace and remove excessive blank lines
        return text.replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
