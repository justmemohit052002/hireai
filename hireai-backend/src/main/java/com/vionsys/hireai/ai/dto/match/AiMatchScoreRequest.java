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
public class AiMatchScoreRequest {
    private List<String> resumeSkills;
    private List<String> jobSkills;
}
