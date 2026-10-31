package com.vionsys.hireai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vionsys.hireai.application.config.AtsProperties;
import com.vionsys.hireai.application.dto.AtsMatchResult;
import com.vionsys.hireai.application.dto.llm.LlmAtsResponse;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.entity.Resume;
import com.vionsys.hireai.candidate.entity.Skill;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.enums.ExperienceLevel;

@ExtendWith(MockitoExtension.class)
class AtsMatchScoringServiceUnitTest {

    @Mock
    private LlmAtsClient llmAtsClient;

    @Spy
    private AtsProperties atsProperties = new AtsProperties();

    @InjectMocks
    private AtsMatchScoringService atsMatchScoringService;

    private Candidate candidate;
    private Job job;

    @BeforeEach
    void setUp() {
        Skill javaSkill = Skill.builder().id(UUID.randomUUID()).name("Java").build();
        Skill springSkill = Skill.builder().id(UUID.randomUUID()).name("Spring Boot").build();
        Skill postgresSkill = Skill.builder().id(UUID.randomUUID()).name("PostgreSQL").build();

        Resume resume = Resume.builder()
                .rawText("Proficient in Java, Spring Boot, and PostgreSQL database development.")
                .build();

        candidate = Candidate.builder()
                .id(UUID.randomUUID())
                .candidateId("CAND-001")
                .currentDesignation("Software Engineer")
                .experience(new BigDecimal("3.0"))
                .skills(Set.of(javaSkill, springSkill, postgresSkill))
                .resume(resume)
                .build();

        job = Job.builder()
                .id(UUID.randomUUID())
                .title("Java Software Engineer")
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .skills(List.of("Java", "Spring Boot", "PostgreSQL"))
                .preferredSkills(List.of("Docker", "AWS"))
                .build();
    }

    // =========================================================================
    // 1. NORMAL INPUT
    // =========================================================================

    @Test
    @DisplayName("shouldComputeMatchScore_whenNormalInputProvided")
    void shouldComputeMatchScore_whenNormalInputProvided() {
        // Arrange: LLM is unavailable, fallback to rule-based
        when(llmAtsClient.scoreWithLlm(candidate, job)).thenReturn(Optional.empty());

        // Act
        AtsMatchResult result = atsMatchScoringService.computeAtsScore(candidate, job);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMatchScore()).isBetween(0, 100);
        assertThat(result.getMatchingSkills()).contains("Java", "Spring Boot", "PostgreSQL");
    }

    // =========================================================================
    // 2. EMPTY OR NULL INPUT
    // =========================================================================

    @Test
    @DisplayName("shouldHandleNullOrEmptyInputSafely_andReturnValidScore")
    void shouldHandleNullOrEmptyInputSafely_andReturnValidScore() {
        // Arrange
        Candidate emptyCandidate = Candidate.builder()
                .id(UUID.randomUUID())
                .candidateId("CAND-EMPTY")
                .skills(null)
                .resume(null)
                .currentDesignation(null)
                .build();

        Job emptyJob = Job.builder()
                .id(UUID.randomUUID())
                .skills(null)
                .preferredSkills(null)
                .title(null)
                .build();

        when(llmAtsClient.scoreWithLlm(emptyCandidate, emptyJob)).thenReturn(Optional.empty());

        // Act
        AtsMatchResult result = atsMatchScoringService.computeAtsScore(emptyCandidate, emptyJob);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMatchScore()).isBetween(0, 100);
    }

    // =========================================================================
    // 3. VERY LONG INPUT
    // =========================================================================

    @Test
    @DisplayName("shouldHandleVeryLongInputSafely")
    void shouldHandleVeryLongInputSafely() {
        // Arrange: 100 job skills and large resume text
        List<String> largeSkillList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeSkillList.add("TechnologySkill_" + i);
        }
        job.setSkills(largeSkillList);

        StringBuilder longResume = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longResume.append("Experienced in TechnologySkill_").append(i).append(" with hands-on practice. ");
        }
        candidate.setResume(Resume.builder().rawText(longResume.toString()).build());

        when(llmAtsClient.scoreWithLlm(candidate, job)).thenReturn(Optional.empty());

        // Act
        AtsMatchResult result = atsMatchScoringService.computeAtsScore(candidate, job);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMatchScore()).isBetween(0, 100);
    }

    // =========================================================================
    // 4. SPECIAL CHARACTERS IN SKILLS
    // =========================================================================

    @Test
    @DisplayName("shouldHandleSpecialCharactersInSkillsCorrectly")
    void shouldHandleSpecialCharactersInSkillsCorrectly() {
        // Arrange: Skills with C++, C#, .NET, Node.js
        Skill cpp = Skill.builder().id(UUID.randomUUID()).name("C++").build();
        Skill csharp = Skill.builder().id(UUID.randomUUID()).name("C#").build();
        Skill dotnet = Skill.builder().id(UUID.randomUUID()).name(".NET").build();
        Skill node = Skill.builder().id(UUID.randomUUID()).name("Node.js").build();

        candidate.setSkills(Set.of(cpp, csharp, dotnet, node));
        candidate.setResume(Resume.builder().rawText("Built backend systems in C++, C#, .NET Core, and Node.js.").build());

        job.setSkills(List.of("C++", "C#", ".NET", "Node.js"));
        job.setPreferredSkills(Collections.emptyList());

        when(llmAtsClient.scoreWithLlm(candidate, job)).thenReturn(Optional.empty());

        // Act
        AtsMatchResult result = atsMatchScoringService.computeAtsScore(candidate, job);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMatchScore()).isBetween(0, 100);
    }

    // =========================================================================
    // 5. SCORE BOUNDARIES (ALWAYS 0 TO 100)
    // =========================================================================

    @Test
    @DisplayName("shouldClampScoreBetween0And100_whenLlmReturnsExceededScore")
    void shouldClampScoreBetween0And100_whenLlmReturnsExceededScore() {
        // Arrange: LLM returns score of 150 (out of bounds)
        LlmAtsResponse highResponse = LlmAtsResponse.builder()
                .atsScore(150)
                .matchingSkills(List.of("Java"))
                .missingSkills(List.of())
                .build();
        when(llmAtsClient.scoreWithLlm(candidate, job)).thenReturn(Optional.of(highResponse));

        // Act
        AtsMatchResult result = atsMatchScoringService.computeAtsScore(candidate, job);

        // Assert: Clamped to 100
        assertThat(result.getMatchScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("shouldClampScoreBetween0And100_whenLlmReturnsNegativeScore")
    void shouldClampScoreBetween0And100_whenLlmReturnsNegativeScore() {
        // Arrange: LLM returns negative score
        LlmAtsResponse lowResponse = LlmAtsResponse.builder()
                .atsScore(-25)
                .matchingSkills(List.of())
                .missingSkills(List.of("Java"))
                .build();
        when(llmAtsClient.scoreWithLlm(candidate, job)).thenReturn(Optional.of(lowResponse));

        // Act
        AtsMatchResult result = atsMatchScoringService.computeAtsScore(candidate, job);

        // Assert: Clamped to 0
        assertThat(result.getMatchScore()).isEqualTo(0);
    }
}
