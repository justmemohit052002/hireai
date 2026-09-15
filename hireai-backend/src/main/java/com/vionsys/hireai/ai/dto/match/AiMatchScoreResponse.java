package com.vionsys.hireai.ai.dto.match;

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
public class AiMatchScoreResponse {
    private int matchScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String autoAction; // "shortlist" | "review" | "reject"
}
