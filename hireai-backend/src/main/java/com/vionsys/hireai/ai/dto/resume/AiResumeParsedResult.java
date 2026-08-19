package com.vionsys.hireai.ai.dto.resume;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResumeParsedResult {
    private String candidateId;
    private List<String> skills;
    private Double yearsExperience;
    private List<EducationDto> education;
    private List<ProjectDto> projects;
    private String domain;
    private String currentRole;
    private String parseStatus;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationDto {
        private String degree;
        private String institution;
        private String year;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectDto {
        private String name;
        private String description;
    }
}
