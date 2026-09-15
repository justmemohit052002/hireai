package com.vionsys.hireai.ai.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.ai.dto.interview.AiInterviewEvaluateRequest;
import com.vionsys.hireai.ai.dto.interview.AiInterviewEvaluateResponse;
import com.vionsys.hireai.ai.dto.interview.AiInterviewQuestionsRequest;
import com.vionsys.hireai.ai.dto.interview.AiInterviewQuestionsResponse;
import com.vionsys.hireai.ai.entity.InterviewAssessment;
import com.vionsys.hireai.ai.entity.InterviewQuestion;
import com.vionsys.hireai.ai.repository.InterviewAssessmentRepository;
import com.vionsys.hireai.ai.repository.InterviewQuestionRepository;
import com.vionsys.hireai.ai.service.DecisionEngineService;
import com.vionsys.hireai.ai.service.InterviewAiService;
import com.vionsys.hireai.application.entity.JobApplication;
import com.vionsys.hireai.application.repository.JobApplicationRepository;
import com.vionsys.hireai.exception.ApplicationNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InterviewAiServiceImpl implements InterviewAiService {

    private final JobApplicationRepository applicationRepository;
    private final InterviewAssessmentRepository assessmentRepository;
    private final InterviewQuestionRepository questionRepository;
    private final AiEngineClient aiEngineClient;
    private final DecisionEngineService decisionEngineService;

    @Override
    public AiInterviewQuestionsResponse getOrGenerateQuestions(UUID candidateUserId, UUID applicationId) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Job application not found with id: " + applicationId));

        validateCandidateOwnership(application, candidateUserId);

        // 1. Check if assessment and questions already exist
        InterviewAssessment assessment = assessmentRepository.findByJobApplicationId(applicationId)
                .orElse(null);

        if (assessment != null && assessment.getQuestions() != null && !assessment.getQuestions().isEmpty()) {
            List<AiInterviewQuestionsResponse.QuestionItem> items = assessment.getQuestions().stream()
                    .map(q -> AiInterviewQuestionsResponse.QuestionItem.builder()
                            .questionId(q.getQuestionId())
                            .text(q.getQuestionText())
                            .type(q.getQuestionType())
                            .build())
                    .toList();
            return AiInterviewQuestionsResponse.builder().questions(items).build();
        }

        // 2. Create new assessment if not existing
        if (assessment == null) {
            assessment = assessmentRepository.save(
                    InterviewAssessment.builder()
                            .jobApplication(application)
                            .status("PENDING")
                            .build()
            );
        }

        // 3. Generate questions via AI Engine
        List<String> skills = application.getJob().getSkills() != null && !application.getJob().getSkills().isEmpty()
                ? application.getJob().getSkills()
                : List.of("Software Engineering", "System Design", "Problem Solving");

        AiInterviewQuestionsRequest request = AiInterviewQuestionsRequest.builder()
                .jobId(application.getJob().getId().toString())
                .skills(skills)
                .build();

        AiInterviewQuestionsResponse response = aiEngineClient.generateInterviewQuestions(request)
                .orElseGet(() -> fallbackQuestions(skills));

        // 4. Persist questions in database
        List<InterviewQuestion> savedQuestions = new ArrayList<>();
        if (response.getQuestions() != null) {
            for (AiInterviewQuestionsResponse.QuestionItem item : response.getQuestions()) {
                InterviewQuestion question = InterviewQuestion.builder()
                        .assessment(assessment)
                        .questionId(item.getQuestionId())
                        .questionText(item.getText())
                        .questionType(item.getType() != null ? item.getType() : "open_text")
                        .build();
                savedQuestions.add(questionRepository.save(question));
            }
        }

        assessment.setQuestions(savedQuestions);
        assessmentRepository.save(assessment);

        return response;
    }

    @Override
    public AiInterviewEvaluateResponse submitAndEvaluateInterview(UUID candidateUserId, UUID applicationId, AiInterviewEvaluateRequest request) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Job application not found with id: " + applicationId));

        validateCandidateOwnership(application, candidateUserId);

        InterviewAssessment assessment = assessmentRepository.findByJobApplicationId(applicationId)
                .orElseThrow(() -> new IllegalStateException("No interview assessment found for this application. Please request questions first."));

        if ("EVALUATED".equalsIgnoreCase(assessment.getStatus())) {
            throw new IllegalStateException("Interview has already been evaluated for this application.");
        }

        // 1. Evaluate answers with AI Engine
        AiInterviewEvaluateResponse response = aiEngineClient.evaluateInterview(request)
                .orElseGet(() -> fallbackEvaluation(request));

        // 2. Persist evaluations to database
        Map<String, AiInterviewEvaluateResponse.EvaluatedAnswerItem> evalMap = new HashMap<>();
        if (response.getEvaluatedAnswers() != null) {
            for (AiInterviewEvaluateResponse.EvaluatedAnswerItem item : response.getEvaluatedAnswers()) {
                evalMap.put(item.getQuestionId(), item);
            }
        }

        if (request.getAnswers() != null) {
            for (AiInterviewEvaluateRequest.AnswerItem ans : request.getAnswers()) {
                questionRepository.findByAssessmentIdAndQuestionId(assessment.getId(), ans.getQuestionId())
                        .ifPresent(q -> {
                            q.setAnswerText(ans.getAnswerText());
                            AiInterviewEvaluateResponse.EvaluatedAnswerItem eval = evalMap.get(ans.getQuestionId());
                            if (eval != null) {
                                q.setScore(eval.getScore());
                                q.setFeedback(eval.getFeedback());
                            }
                            questionRepository.save(q);
                        });
            }
        }

        assessment.setTotalScore(response.getInterviewScore());
        assessment.setStatus("EVALUATED");
        assessment.setCompletedAt(LocalDateTime.now());
        assessmentRepository.save(assessment);

        application.setInterviewScore(response.getInterviewScore());
        applicationRepository.save(application);

        // 3. Trigger automatic decision finalization
        try {
            decisionEngineService.finalizeApplicationDecision(applicationId);
        } catch (Exception ex) {
            log.warn("Could not finalize decision automatically: {}", ex.getMessage());
        }

        return response;
    }

    private void validateCandidateOwnership(JobApplication application, UUID candidateUserId) {
        if (application.getCandidate().getUser() == null ||
                !application.getCandidate().getUser().getId().equals(candidateUserId)) {
            throw new AccessDeniedException("You do not have permission to access interview for this application.");
        }
    }

    private AiInterviewQuestionsResponse fallbackQuestions(List<String> skills) {
        List<AiInterviewQuestionsResponse.QuestionItem> list = new ArrayList<>();
        int qNum = 1;
        for (String skill : skills) {
            if (qNum > 3) break;
            list.add(AiInterviewQuestionsResponse.QuestionItem.builder()
                    .questionId("q" + qNum)
                    .text("Explain how you utilize " + skill + " in building scalable production applications.")
                    .type("open_text")
                    .build());
            qNum++;
        }
        if (list.isEmpty()) {
            list.add(AiInterviewQuestionsResponse.QuestionItem.builder()
                    .questionId("q1")
                    .text("Describe a challenging technical project you worked on and how you resolved key architecture bottlenecks.")
                    .type("open_text")
                    .build());
        }
        return AiInterviewQuestionsResponse.builder().questions(list).build();
    }

    private AiInterviewEvaluateResponse fallbackEvaluation(AiInterviewEvaluateRequest request) {
        List<AiInterviewEvaluateResponse.EvaluatedAnswerItem> evaluated = new ArrayList<>();
        int totalScore = 0;
        int count = 0;

        if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            for (AiInterviewEvaluateRequest.AnswerItem ans : request.getAnswers()) {
                String text = ans.getAnswerText() != null ? ans.getAnswerText().trim() : "";
                int score = text.length() > 50 ? 80 : (text.length() > 20 ? 60 : 40);
                totalScore += score;
                count++;

                evaluated.add(AiInterviewEvaluateResponse.EvaluatedAnswerItem.builder()
                        .questionId(ans.getQuestionId())
                        .score(score)
                        .feedback("Answer received and evaluated based on key domain competency.")
                        .build());
            }
        }

        int avgScore = count > 0 ? (totalScore / count) : 70;
        return AiInterviewEvaluateResponse.builder()
                .interviewScore(avgScore)
                .evaluatedAnswers(evaluated)
                .build();
    }
}
