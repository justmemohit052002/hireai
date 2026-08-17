package com.vionsys.hireai.candidate.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.exception.FileStorageException;

@Service
public class LocalFileStorageService implements FileStorageService{

	 private final Path uploadPath;

	    public LocalFileStorageService(
	            @Value("${file.upload-dir}") String uploadDir) {

	        this.uploadPath = Paths.get(uploadDir)
	                .toAbsolutePath()
	                .normalize();

	        try {
	            Files.createDirectories(this.uploadPath);
	        } catch (IOException ex) {
	            throw new FileStorageException(
	                    "Could not create upload directory."
	            );
	        }
	    }

	    @Override
	    public String store(MultipartFile file) throws IOException {

	        if (file == null || file.isEmpty()) {
	            throw new FileStorageException(
	                    "Cannot store an empty file."
	            );
	        }

	        String originalFileName = file.getOriginalFilename();

	        if (originalFileName == null || originalFileName.isBlank()) {
	            throw new FileStorageException(
	                    "File name is missing."
	            );
	        }

	        String extension = "";

	        int extensionIndex = originalFileName.lastIndexOf(".");

	        if (extensionIndex > 0) {
	            extension = originalFileName.substring(extensionIndex);
	        }

	        String storedFileName = UUID.randomUUID() + extension;

	        Path targetLocation = uploadPath.resolve(storedFileName)
	                .normalize();

	        if (!targetLocation.getParent().equals(uploadPath)) {
	            throw new FileStorageException(
	                    "Invalid file path."
	            );
	        }

	        Files.copy(
	                file.getInputStream(),
	                targetLocation,
	                StandardCopyOption.REPLACE_EXISTING
	        );

	        return targetLocation.toString();
	    }

	    @Override
	    public void delete(String filePath) throws IOException {

	        if (filePath == null || filePath.isBlank()) {
	            return;
	        }

	        Path path = Paths.get(filePath)
	                .toAbsolutePath()
	                .normalize();

	        if (!path.startsWith(uploadPath)) {
	            throw new FileStorageException(
	                    "Invalid file path."
	            );
	        }

	        Files.deleteIfExists(path);
	    }
}
