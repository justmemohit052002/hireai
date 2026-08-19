package com.vionsys.hireai.ai.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.ai.dto.jd.AiJdGenerateRequest;
import com.vionsys.hireai.ai.dto.jd.AiJdGenerateResponse;
import com.vionsys.hireai.ai.service.AiJdService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJdServiceImpl implements AiJdService {

    private final AiEngineClient aiEngineClient;

    @Override
    public AiJdGenerateResponse generateJobDescription(AiJdGenerateRequest request) {
        log.info("Generating AI Job Description for title: {}", request.getJobTitle());

        return aiEngineClient.generateJd(request)
                .orElseGet(() -> fallbackJd(request));
    }

    private AiJdGenerateResponse fallbackJd(AiJdGenerateRequest request) {
        String title = request.getJobTitle() != null ? request.getJobTitle() : "Software Engineer";
        List<String> skills = request.getRequiredSkills() != null ? request.getRequiredSkills() : List.of("Java", "Problem Solving");
        String exp = request.getExperienceLevel() != null ? request.getExperienceLevel() : "Mid-Level";

        return AiJdGenerateResponse.builder()
                .description("We are looking for an experienced " + title + " (" + exp + ") to join our innovative engineering team.")
                .responsibilities(List.of(
                        "Design, build, and maintain efficient, reusable, and reliable services",
                        "Collaborate with cross-functional teams to define and deliver high-impact product features",
                        "Identify bottlenecks and bugs, and devise solutions to these problems"
                ))
                .mustHaveSkills(skills)
                .niceToHaveSkills(List.of("Docker", "Kubernetes", "Cloud Platforms (AWS/GCP)", "CI/CD"))
                .interviewQuestions(List.of(
                        "How do you design and structure microservices for high throughput?",
                        "Describe your approach to handling database locking and transactions."
                ))
                .build();
    }
}
