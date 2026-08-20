package com.vionsys.hireai.ai.service;

import com.vionsys.hireai.ai.dto.jd.AiJdGenerateRequest;
import com.vionsys.hireai.ai.dto.jd.AiJdGenerateResponse;

public interface AiJdService {

    AiJdGenerateResponse generateJobDescription(AiJdGenerateRequest request);
}
