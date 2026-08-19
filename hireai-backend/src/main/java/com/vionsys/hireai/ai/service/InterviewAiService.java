package com.vionsys.hireai.ai.service;

import java.util.UUID;

import com.vionsys.hireai.ai.dto.interview.AiInterviewEvaluateRequest;
import com.vionsys.hireai.ai.dto.interview.AiInterviewEvaluateResponse;
import com.vionsys.hireai.ai.dto.interview.AiInterviewQuestionsResponse;

public interface InterviewAiService {

    AiInterviewQuestionsResponse getOrGenerateQuestions(UUID candidateUserId, UUID applicationId);

    AiInterviewEvaluateResponse submitAndEvaluateInterview(UUID candidateUserId, UUID applicationId, AiInterviewEvaluateRequest request);
}
