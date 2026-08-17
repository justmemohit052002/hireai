package com.vionsys.hireai.candidate.service;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.dto.ResumeResponse;

public interface ResumeService {

	 ResumeResponse uploadResume(
	            UUID candidateId,
	            MultipartFile file
	    );

	    ResumeResponse getResume(UUID candidateId);
        
	    Resource downloadResume(UUID candidateId);
	    
	    void deleteResume(UUID candidateId);
	    
	   
}
