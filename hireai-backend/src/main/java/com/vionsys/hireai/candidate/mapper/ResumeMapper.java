package com.vionsys.hireai.candidate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.vionsys.hireai.candidate.dto.ResumeResponse;
import com.vionsys.hireai.candidate.entity.Resume;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ResumeMapper {

    ResumeResponse toResponse(Resume resume);
}
