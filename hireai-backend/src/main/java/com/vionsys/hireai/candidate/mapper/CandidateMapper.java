package com.vionsys.hireai.candidate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.vionsys.hireai.candidate.dto.CandidateResponse;
import com.vionsys.hireai.candidate.dto.CreateCandidateRequest;
import com.vionsys.hireai.candidate.dto.UpdateCandidateRequest;
import com.vionsys.hireai.candidate.entity.Candidate;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CandidateMapper {

	


	    @Mapping(target = "id", ignore = true)
	    @Mapping(target = "candidateId", ignore = true)
	    @Mapping(target = "candidateStatus", ignore = true)
	    @Mapping(target = "resume", ignore = true)
	    @Mapping(target = "skills", ignore = true)
	    @Mapping(target = "createdAt", ignore = true)
	    @Mapping(target = "updatedAt", ignore = true)
	    @Mapping(target = "createdBy", ignore = true)
	    @Mapping(target = "updatedBy", ignore = true)
	    @Mapping(target = "deleted", ignore = true)
	    @Mapping(target = "version", ignore = true)
		Candidate toEntity(CreateCandidateRequest request);

	    CandidateResponse toResponse(Candidate candidate);
	 
	    @Mapping(target = "id", ignore = true)
	    @Mapping(target = "candidateId", ignore = true)
	    @Mapping(target = "candidateStatus", ignore = true)
	    @Mapping(target = "resume", ignore = true)
	    @Mapping(target = "skills", ignore = true)
	    @Mapping(target = "createdAt", ignore = true)
	    @Mapping(target = "updatedAt", ignore = true)
	    @Mapping(target = "createdBy", ignore = true)
	    @Mapping(target = "updatedBy", ignore = true)
	    @Mapping(target = "deleted", ignore = true)
	    @Mapping(target = "version", ignore = true)
	    void updateCandidate(
	            UpdateCandidateRequest request,
	            @MappingTarget Candidate candidate
	    );

}
