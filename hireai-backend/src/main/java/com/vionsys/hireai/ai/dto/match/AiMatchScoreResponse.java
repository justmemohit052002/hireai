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
public class AiMatchScoreResponse {
    private int matchScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> bonusSkills;
    private String autoAction; // "shortlist" | "review" | "reject"
    private List<AiMatchDetailDto> matchDetails;
    private Double skillCoveragePercent;
    private Double experienceMatchPercent;
    private AiScoreBreakdownDto scoreBreakdown;
    private String rationale;
    private String engineVersion;
    private Boolean semanticModelAvailable;
}
