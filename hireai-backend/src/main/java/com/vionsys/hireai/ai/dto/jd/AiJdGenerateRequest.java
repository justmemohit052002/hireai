package com.vionsys.hireai.ai.dto.jd;

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
public class AiJdGenerateRequest {
    private String jobTitle;
    private List<String> requiredSkills;
    private String experienceLevel;
}
