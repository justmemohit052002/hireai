package com.vionsys.hireai.ai.dto.decision;

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
public class AiDecisionRequest {
    private Double resumeScore;
    private Double interviewScore;
    private Double chatbotSignalScore;
}
