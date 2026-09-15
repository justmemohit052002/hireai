package com.vionsys.hireai.ai.service;

import java.util.UUID;

import com.vionsys.hireai.ai.dto.decision.AiDecisionResponse;

public interface DecisionEngineService {

    AiDecisionResponse finalizeApplicationDecision(UUID applicationId);

    AiDecisionResponse finalizeApplicationDecisionForRecruiter(UUID recruiterUserId, UUID applicationId);
}
