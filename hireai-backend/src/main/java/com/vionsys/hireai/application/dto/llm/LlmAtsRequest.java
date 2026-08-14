package com.vionsys.hireai.application.dto.llm;

import java.math.BigDecimal;
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
public class LlmAtsRequest {

    private CandidateInfo candidate;
    private JobInfo job;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidateInfo {
        private String candidateId;
        private String name;
        private BigDecimal experienceYears;
        private String currentDesignation;
        private String currentCompany;
        private List<String> skills;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobInfo {
        private String jobId;
        private String title;
        private String experienceLevel;
        private List<String> requiredSkills;
        private String description;
    }
}
