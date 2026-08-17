package com.vionsys.hireai.candidate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.vionsys.hireai.candidate.dto.ResumeResponse;
import com.vionsys.hireai.candidate.entity.Resume;

@Mapper(componentModel = "spring")
public interface ResumeMapper {

    @Mapping(target = "uploadedAt", source = "createdAt")
    ResumeResponse toResponse(Resume resume);
}
