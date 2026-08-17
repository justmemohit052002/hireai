package com.vionsys.hireai.candidate.service.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.dto.ResumeResponse;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.entity.Resume;
import com.vionsys.hireai.candidate.enums.ResumeStatus;
import com.vionsys.hireai.candidate.exception.CandidateNotFoundException;
import com.vionsys.hireai.candidate.exception.DuplicateResourceException;
import com.vionsys.hireai.candidate.exception.FileStorageException;
import com.vionsys.hireai.candidate.exception.ResumeNotFoundException;
import com.vionsys.hireai.candidate.mapper.ResumeMapper;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.candidate.repository.ResumeRepository;
import com.vionsys.hireai.candidate.service.ResumeService;
import com.vionsys.hireai.candidate.storage.FileStorageService;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
@Transactional
public class ResumeServiceImpl implements ResumeService {
    
	@Value("${resume.max-file-size}")
	private long maxFileSize;
	
	    private final CandidateRepository candidateRepository;
	    private final ResumeRepository resumeRepository;
	    private final ResumeMapper resumeMapper;
	    private final FileStorageService fileStorageService;

	    @Override
	    public ResumeResponse uploadResume(
	            UUID candidateId,
	            MultipartFile file) {

	        // 1. Find candidate
	        Candidate candidate = candidateRepository.findById(candidateId)
	                .orElseThrow(() ->
	                        new CandidateNotFoundException(
	                                "Candidate not found with id : " + candidateId
	                        )
	                );

	        // 2. Validate file
	        validateFile(file);

	        // 3. Check whether candidate already has a resume
	        if (resumeRepository.existsByCandidateIdAndDeletedFalse(candidateId)) {
	            throw new DuplicateResourceException(
	                    "Resume already exists for candidate with id : "
	                            + candidateId
	            );
	        }

	        try {

	            // 4. Store physical file
	            String filePath = fileStorageService.store(file);

	            // 5. Create Resume entity
	            Resume resume = new Resume();

	            resume.setOriginalFileName(file.getOriginalFilename());
	            resume.setStoredFileName(
	                    extractFileName(filePath)
	            );
	            resume.setFileType(file.getContentType());
	            resume.setFileSize(file.getSize());
	            resume.setFilePath(filePath);
	            resume.setUploadedAt(LocalDateTime.now());
	            resume.setResumeStatus(ResumeStatus.UPLOADED);
	            resume.setCandidate(candidate);

	            // 6. Save Resume
	            Resume savedResume = resumeRepository.save(resume);

	            // 7. Convert Entity → Response DTO
	            return resumeMapper.toResponse(savedResume);

	        } catch (IOException ex) {

	            throw new FileStorageException(
	                    "Failed to store resume file.",
	                    ex
	            );
	        }
	    }

	    private void validateFile(MultipartFile file) {

	        if (file == null || file.isEmpty()) {
	            throw new FileStorageException(
	                    "Resume file cannot be empty."
	            );
	        }
	        
	        if (file.getSize() > maxFileSize) {
	            throw new FileStorageException(
	                    "Resume file size must not exceed 5 MB."
	            );
	        }

	        String fileName = file.getOriginalFilename();

	        if (fileName == null || fileName.isBlank()) {
	            throw new FileStorageException(
	                    "Resume file name is missing."
	            );
	        }

	        String contentType = file.getContentType();

	        boolean validPdf =
	                "application/pdf".equalsIgnoreCase(contentType);

	        boolean validDocx =
	                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
	                        .equalsIgnoreCase(contentType);

	        if (!validPdf && !validDocx) {
	            throw new FileStorageException(
	                    "Only PDF and DOCX resume files are allowed."
	            );
	        }
	    }

	    private String extractFileName(String filePath) {

	    	   return java.nio.file.Paths
	    	            .get(filePath)
	    	            .getFileName()
	    	            .toString();
	    }

	    @Override
	    @Transactional(readOnly = true)
	    public ResumeResponse getResume(UUID candidateId) {
	    	 
	    	Resume resume = resumeRepository.findByCandidateIdAndDeletedFalse(candidateId)
	    	            .orElseThrow(() -> new ResumeNotFoundException(
	    	                    "Resume not found for candidate with id : " + candidateId
	    	            ));

	    	    return resumeMapper.toResponse(resume);
	    }
	    
	    @Override
	    @Transactional(readOnly = true)
	    public Resource downloadResume(UUID candidateId) {

	        Resume resume = resumeRepository.findByCandidateIdAndDeletedFalse(candidateId)
	                .orElseThrow(() -> new ResumeNotFoundException(
	                        "Resume not found for candidate with id : " + candidateId
	                ));
	        
	        String filePath = resume.getFilePath();

	        if (filePath == null || filePath.isBlank()) {
	            throw new FileStorageException(
	                    "Resume file path is missing for candidate with id : "
	                            + candidateId
	            );
	        }
	        
	        try {
	            Path path = Paths.get(resume.getFilePath())
	                    .toAbsolutePath()
	                    .normalize();

	            Resource resource = new UrlResource(path.toUri());

	            if (!resource.exists() || !resource.isReadable()) {
	                throw new FileStorageException(
	                        "Resume file could not be found or is not readable."
	                );
	            }

	            return resource;

	        } catch (MalformedURLException ex) {
	            throw new FileStorageException(
	                    "Could not load resume file.",
	                    ex
	            );
	        }
	    }

	    
	    @Override
	    @Transactional
	    public void deleteResume(UUID candidateId) {

	        Resume resume = resumeRepository
	                .findByCandidateIdAndDeletedFalse(candidateId)
	                .orElseThrow(() ->
	                        new ResumeNotFoundException(
	                                "Resume not found for candidate with id : "
	                                        + candidateId
	                        )
	                );

	        String filePath = resume.getFilePath();

	        if (filePath == null || filePath.isBlank()) {
	            throw new FileStorageException(
	                    "Resume file path is missing for candidate with id : "
	                            + candidateId
	            );
	        }

	        try {

	            Path path = Paths.get(filePath)
	                    .toAbsolutePath()
	                    .normalize();

	            Files.deleteIfExists(path);

	        } catch (IOException | SecurityException ex) {

	            throw new FileStorageException(
	                    "Failed to delete resume file for candidate with id : "
	                            + candidateId,
	                    ex
	            );
	        }

	        // Soft delete database record
	        resume.setDeleted(true);
	        resume.setResumeStatus(ResumeStatus.DELETED);

	        resumeRepository.save(resume);
	    }
}
