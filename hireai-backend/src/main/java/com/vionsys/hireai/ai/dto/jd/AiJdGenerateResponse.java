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
public class AiJdGenerateResponse {
    private String description;
    private List<String> responsibilities;
    private List<String> mustHaveSkills;
    private List<String> niceToHaveSkills;
    private List<String> interviewQuestions;
}
