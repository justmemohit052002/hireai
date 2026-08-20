package com.vionsys.hireai.ai.dto.interview;

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
public class AiInterviewQuestionsRequest {
    private String jobId;
    private List<String> skills;
}
