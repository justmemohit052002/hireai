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
public class AiInterviewEvaluateRequest {
    private String candidateId;
    private List<AnswerItem> answers;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerItem {
        private String questionId;
        private String answerText;
    }
}
