package com.vionsys.hireai.candidate.storage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.exception.FileStorageException;

import lombok.extern.slf4j.Slf4j;

/**
 * Security validator for uploaded files across the HireAI platform.
 * Enforces deep validation across three critical dimensions:
 * 1. File Type (Extension Whitelisting + MIME Type Verification)
 * 2. File Size (Configurable byte limits per upload category)
 * 3. File Content (Magic Bytes, Apache Tika deep inspection, ImageIO decoding,
 *    and rejection of executable/script signatures)
 */
@Slf4j
@Component
public class FileSecurityValidator {

    private final Tika tika = new Tika();

    private final long maxResumeSize;
    private final long maxPhotoSize;
    private final long maxAttachmentSize;

    public static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            ".pdf", ".docx", ".doc"
    )));

    public static final Set<String> ALLOWED_DOCUMENT_MIME_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "application/x-tika-msoffice",
            "application/x-tika-ooxml"
    )));

    public static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".webp", ".gif"
    )));

    public static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    )));

    private static final Set<String> DANGEROUS_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "exe", "dll", "so", "dylib", "sh", "bash", "bat", "cmd", "com", "ps1",
            "vbs", "js", "jsp", "jspx", "php", "phtml", "py", "pyc", "rb", "pl",
            "cgi", "asp", "aspx", "jar", "war", "ear", "class", "scr", "msi", "htm",
            "html", "xhtml", "svg", "xml"
    )));

    // Magic bytes signatures
    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF87_MAGIC = new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61}; // GIF87a
    private static final byte[] GIF89_MAGIC = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61}; // GIF89a
    private static final byte[] ZIP_PK_MAGIC = new byte[]{0x50, 0x4B, 0x03, 0x04}; // PK\x03\x04 for DOCX
    private static final byte[] OLE_MAGIC = new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}; // OLE CFB for DOC

    public FileSecurityValidator(
            @Value("${resume.max-file-size:10485760}") long maxResumeSize,
            @Value("${photo.max-file-size:5242880}") long maxPhotoSize,
            @Value("${chat.attachment.max-file-size:10485760}") long maxAttachmentSize) {
        this.maxResumeSize = maxResumeSize;
        this.maxPhotoSize = maxPhotoSize;
        this.maxAttachmentSize = maxAttachmentSize;
    }

    /**
     * Comprehensive validation for resume files (PDF, DOCX, DOC).
     */
    public void validateResumeFile(MultipartFile file) {
        validateBasicFileProperties(file, maxResumeSize, "Resume");

        String filename = file.getOriginalFilename();
        String extension = getSanitizedExtension(filename, ALLOWED_DOCUMENT_EXTENSIONS);

        byte[] headerBytes = readHeaderBytes(file, 4096);
        inspectForExecutableOrMaliciousSignatures(headerBytes, filename);

        // Magic byte verification for documents
        if (extension.equals(".pdf")) {
            if (!startsWithBytes(headerBytes, PDF_MAGIC)) {
                log.warn("Security rejection: File {} has .pdf extension but invalid PDF magic header", filename);
                throw new FileStorageException("File content does not match a valid PDF document signature.");
            }
        } else if (extension.equals(".docx")) {
            if (!startsWithBytes(headerBytes, ZIP_PK_MAGIC)) {
                log.warn("Security rejection: File {} has .docx extension but invalid ZIP/PK magic header", filename);
                throw new FileStorageException("File content does not match a valid DOCX document signature.");
            }
        } else if (extension.equals(".doc")) {
            if (!startsWithBytes(headerBytes, OLE_MAGIC)) {
                log.warn("Security rejection: File {} has .doc extension but invalid OLE CFB magic header", filename);
                throw new FileStorageException("File content does not match a valid DOC document signature.");
            }
        }

        // Apache Tika deep content inspection
        String detectedMime = detectMimeType(file, headerBytes);
        log.debug("Tika detected MIME type '{}' for file '{}'", detectedMime, filename);

        if (!ALLOWED_DOCUMENT_MIME_TYPES.contains(detectedMime)) {
            // Check if Tika detected an allowed subtype or if it's a valid openxml/zip document
            boolean isAllowedDoc = detectedMime.equals("application/pdf")
                    || (extension.equals(".docx") && (detectedMime.contains("wordprocessingml") || detectedMime.contains("zip") || detectedMime.contains("ooxml")))
                    || (extension.equals(".doc") && (detectedMime.contains("msword") || detectedMime.contains("msoffice")));

            if (!isAllowedDoc) {
                log.warn("Security rejection: File {} detected MIME type '{}' is not an allowed document format", filename, detectedMime);
                throw new FileStorageException("File content is not a supported resume document (detected type: " + detectedMime + ").");
            }
        }
    }

    /**
     * Comprehensive validation for candidate and recruiter profile photos.
     */
    public void validateProfilePhoto(MultipartFile file) {
        validateBasicFileProperties(file, maxPhotoSize, "Profile photo");

        String filename = file.getOriginalFilename();
        String extension = getSanitizedExtension(filename, ALLOWED_IMAGE_EXTENSIONS);

        byte[] headerBytes = readHeaderBytes(file, 4096);
        inspectForExecutableOrMaliciousSignatures(headerBytes, filename);
        inspectImageForEmbeddedScripts(headerBytes, filename);

        // Magic byte verification for images
        boolean validMagic = false;
        if (extension.equals(".png") && startsWithBytes(headerBytes, PNG_MAGIC)) {
            validMagic = true;
        } else if ((extension.equals(".jpg") || extension.equals(".jpeg")) && startsWithBytes(headerBytes, JPEG_MAGIC)) {
            validMagic = true;
        } else if (extension.equals(".gif") && (startsWithBytes(headerBytes, GIF87_MAGIC) || startsWithBytes(headerBytes, GIF89_MAGIC))) {
            validMagic = true;
        } else if (extension.equals(".webp") && isWebPMagic(headerBytes)) {
            validMagic = true;
        }

        if (!validMagic) {
            log.warn("Security rejection: Profile photo {} failed magic byte signature check for extension {}", filename, extension);
            throw new FileStorageException("File content does not match a valid image signature for format: " + extension);
        }

        // Tika MIME verification
        String detectedMime = detectMimeType(file, headerBytes);
        if (!ALLOWED_IMAGE_MIME_TYPES.contains(detectedMime)) {
            log.warn("Security rejection: Profile photo {} detected MIME '{}' is not an allowed image format", filename, detectedMime);
            throw new FileStorageException("File content is not an accepted image format (detected: " + detectedMime + "). Allowed: JPG, PNG, WEBP, GIF.");
        }

        // Deep raster decode verification for standard formats (PNG, JPEG, GIF)
        if (extension.equals(".png") || extension.equals(".jpg") || extension.equals(".jpeg") || extension.equals(".gif")) {
            verifyImageDecode(file, filename);
        }
    }

    /**
     * Comprehensive validation for chat attachments.
     * Supports both document formats (PDF, DOCX, DOC) and image formats (JPG, JPEG, PNG, WEBP, GIF).
     */
    public void validateChatAttachment(MultipartFile file) {
        validateBasicFileProperties(file, maxAttachmentSize, "Chat attachment");

        String filename = file.getOriginalFilename();
        String ext = extractExtension(filename);

        if (ALLOWED_DOCUMENT_EXTENSIONS.contains(ext)) {
            validateResumeFile(file);
        } else if (ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            validateProfilePhoto(file);
        } else {
            log.warn("Security rejection: Chat attachment {} has unsupported extension '{}'", filename, ext);
            throw new FileStorageException("Unsupported attachment format '" + ext + "'. Allowed formats: PDF, DOC, DOCX, JPG, PNG, WEBP, GIF.");
        }
    }

    /**
     * Validates basic file metadata: presence, non-empty, size, and sanitization of filename.
     */
    private void validateBasicFileProperties(MultipartFile file, long maxSize, String category) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException(category + " file cannot be empty.");
        }

        if (file.getSize() > maxSize) {
            long maxMb = maxSize / (1024 * 1024);
            throw new FileStorageException(String.format("%s file size (%d bytes) exceeds maximum limit of %d MB.", category, file.getSize(), maxMb));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new FileStorageException(category + " file name is missing.");
        }

        // Reject null bytes, path traversal sequences, and control characters
        if (filename.contains("\0") || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            log.warn("Security rejection: Attempted path traversal or null byte injection in filename: {}", filename);
            throw new FileStorageException("Invalid characters or path traversal attempt detected in file name.");
        }

        // Reject dangerous secondary extensions (e.g., shell.php.pdf, payload.exe.jpg)
        String[] parts = filename.toLowerCase(Locale.ROOT).split("\\.");
        if (parts.length > 2) {
            for (int i = 1; i < parts.length - 1; i++) {
                if (DANGEROUS_EXTENSIONS.contains(parts[i])) {
                    log.warn("Security rejection: File {} contains dangerous intermediate extension '{}'", filename, parts[i]);
                    throw new FileStorageException("Dangerous secondary file extension detected: ." + parts[i]);
                }
            }
        }
    }

    /**
     * Safely reads the initial byte header from a MultipartFile without closing or corrupting the stream.
     */
    public byte[] readHeaderBytes(MultipartFile file, int maxBytes) {
        try (InputStream is = file.getInputStream()) {
            byte[] buffer = new byte[maxBytes];
            int read = is.read(buffer);
            if (read <= 0) {
                return new byte[0];
            }
            return Arrays.copyOf(buffer, read);
        } catch (IOException ex) {
            log.error("Failed to read header bytes from uploaded file {}: {}", file.getOriginalFilename(), ex.getMessage());
            throw new FileStorageException("Failed to read file content for security inspection.", ex);
        }
    }

    /**
     * Detects MIME type using Apache Tika purely from magic byte inspection.
     */
    public String detectMimeType(MultipartFile file, byte[] headerBytes) {
        try {
            if (headerBytes != null && headerBytes.length > 0) {
                return tika.detect(headerBytes).toLowerCase(Locale.ROOT);
            }
            try (InputStream is = file.getInputStream()) {
                return tika.detect(is).toLowerCase(Locale.ROOT);
            }
        } catch (Exception ex) {
            log.warn("Tika MIME detection failed for file {}: {}", file.getOriginalFilename(), ex.getMessage());
            return "application/octet-stream";
        }
    }

    /**
     * Inspects header bytes for executable signatures (PE, ELF, Mach-O, Shebang, Java bytecode).
     */
    private void inspectForExecutableOrMaliciousSignatures(byte[] bytes, String filename) {
        if (bytes == null || bytes.length < 2) {
            return;
        }

        // 1. Windows PE / DOS Executable ("MZ")
        if (bytes[0] == 0x4D && bytes[1] == 0x5A) {
            log.warn("Security violation: Executable MZ signature detected in file {}", filename);
            throw new FileStorageException("Executable files (Windows PE/EXE) are strictly prohibited.");
        }

        // 2. Linux ELF Executable ("\x7fELF")
        if (bytes.length >= 4 && bytes[0] == 0x7F && bytes[1] == 0x45 && bytes[2] == 0x4C && bytes[3] == 0x46) {
            log.warn("Security violation: Linux ELF signature detected in file {}", filename);
            throw new FileStorageException("Executable files (Linux ELF binary) are strictly prohibited.");
        }

        // 3. Unix Shell Script Shebang ("#!")
        if (bytes[0] == 0x23 && bytes[1] == 0x21) {
            log.warn("Security violation: Shell script shebang '#!' detected in file {}", filename);
            throw new FileStorageException("Executable script files are strictly prohibited.");
        }

        // 4. Java Class Bytecode (0xCA, 0xFE, 0xBA, 0xBE)
        if (bytes.length >= 4 && bytes[0] == (byte) 0xCA && bytes[1] == (byte) 0xFE
                && bytes[2] == (byte) 0xBA && bytes[3] == (byte) 0xBE) {
            log.warn("Security violation: Java bytecode signature detected in file {}", filename);
            throw new FileStorageException("Compiled executable bytecode is strictly prohibited.");
        }
    }

    /**
     * Inspects image header bytes for embedded script tags (PHP, JS, HTML, SVG) that could cause Stored XSS.
     */
    private void inspectImageForEmbeddedScripts(byte[] bytes, String filename) {
        if (bytes == null || bytes.length == 0) {
            return;
        }

        String headerText = new String(bytes, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
        String[] forbiddenPatterns = {
                "<script", "<?php", "<%", "<!doctype html", "<html", "<body",
                "<svg", "javascript:", "eval(", "base64_decode"
        };

        for (String pattern : forbiddenPatterns) {
            if (headerText.contains(pattern)) {
                log.warn("Security violation: Embedded script pattern '{}' found in image {}", pattern, filename);
                throw new FileStorageException("Malicious script signature detected in file header.");
            }
        }
    }

    /**
     * Verifies that the raster image decodes into a valid renderable image via Java ImageIO.
     */
    private void verifyImageDecode(MultipartFile file, String filename) {
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                log.warn("Security rejection: ImageIO failed to decode image {} (returned null)", filename);
                throw new FileStorageException("The uploaded image is corrupt or not a valid image format.");
            }
            if (image.getWidth() <= 0 || image.getHeight() <= 0) {
                log.warn("Security rejection: Image {} has invalid dimensions {}x{}", filename, image.getWidth(), image.getHeight());
                throw new FileStorageException("Invalid image dimensions.");
            }
        } catch (IOException ex) {
            log.error("Image decode error for {}: {}", filename, ex.getMessage());
            throw new FileStorageException("Failed to decode image file: " + ex.getMessage(), ex);
        }
    }

    private boolean isWebPMagic(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return false;
        }
        // RIFF at 0..3 and WEBP at 8..11
        boolean riff = bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46;
        boolean webp = bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50;
        return riff && webp;
    }

    private boolean startsWithBytes(byte[] source, byte[] target) {
        if (source == null || target == null || source.length < target.length) {
            return false;
        }
        for (int i = 0; i < target.length; i++) {
            if (source[i] != target[i]) {
                return false;
            }
        }
        return true;
    }

    public String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int extIndex = filename.lastIndexOf(".");
        if (extIndex >= 0) {
            return filename.substring(extIndex).toLowerCase(Locale.ROOT).trim();
        }
        return "";
    }

    public String getSanitizedExtension(String filename, Set<String> allowedExtensions) {
        String ext = extractExtension(filename);
        if (!allowedExtensions.contains(ext)) {
            log.warn("Security rejection: Unsupported file extension '{}' in filename '{}'", ext, filename);
            throw new FileStorageException("Unsupported file format: '" + ext + "'. Allowed formats: " + allowedExtensions);
        }
        return ext;
    }
}
