package com.vionsys.hireai.ai.dto.match;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiMatchScoreRequest {
    private List<String> resumeSkills;
    private List<String> jobSkills;
    private List<String> niceToHaveSkills;
    private String resumeText;
    private Double candidateExperienceYears;
    private Double requiredExperienceYears;
    private Double shortlistThreshold;
    private String candidateRole;
    private String jobTitle;
    private String domain;
}
