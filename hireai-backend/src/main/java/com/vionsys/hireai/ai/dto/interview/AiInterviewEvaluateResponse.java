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
public class AiInterviewEvaluateResponse {
    private int interviewScore;
    private List<EvaluatedAnswerItem> evaluatedAnswers;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluatedAnswerItem {
        private String questionId;
        private int score;
        private String feedback;
    }
}
