package com.vionsys.hireai.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
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
import com.vionsys.hireai.candidate.entity.Skill;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.enums.ExperienceLevel;

@ExtendWith(MockitoExtension.class)
class AtsMatchScoringServiceTest {

    @Mock
    private LlmAtsClient llmAtsClient;

    private AtsProperties atsProperties = new AtsProperties();

    private AtsMatchScoringService scoringService;

    private Candidate candidate;
    private Job job;

    @BeforeEach
    void setUp() {
        scoringService = new AtsMatchScoringService(llmAtsClient, atsProperties);
        Skill java = Skill.builder().id(UUID.randomUUID()).name("Java (Core Java, Java 8+)").build();
        Skill spring = Skill.builder().id(UUID.randomUUID()).name("Spring Boot").build();
        Skill postgres = Skill.builder().id(UUID.randomUUID()).name("PostgreSQL").build();
        Skill dsa = Skill.builder().id(UUID.randomUUID()).name("DSA").build();
        Skill aws = Skill.builder().id(UUID.randomUUID()).name("AWS (EC2)").build();

        candidate = Candidate.builder()
                .id(UUID.randomUUID())
                .candidateId("CAND-001")
                .firstName("Mohit")
                .lastName("Kumar")
                .skills(Set.of(java, spring, postgres, dsa, aws))
                .experience(BigDecimal.valueOf(4.0))
                .currentDesignation("Senior Java Developer")
                .build();

        job = Job.builder()
                .id(UUID.randomUUID())
                .title("Senior Java Backend Engineer")
                .skills(List.of("Java programming language", "Spring Boot", "Cloud computing platforms (e.g. AWS)", "Data structures and algorithms", "MongoDB"))
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .build();
    }

    @Test
    void testComputeScore_WithLlmResponse_CarriesSeparatePercentages() {
        LlmAtsResponse mockLlm = LlmAtsResponse.builder()
                .atsScore(88)
                .matchingSkills(List.of("Java programming language", "Spring Boot", "Cloud computing platforms (e.g. AWS)", "Data structures and algorithms"))
                .missingSkills(List.of("MongoDB"))
                .skillCoveragePercent(80.0)
                .experienceMatchPercent(100.0)
                .recommendation("shortlist")
                .build();

        when(llmAtsClient.scoreWithLlm(any(), any())).thenReturn(Optional.of(mockLlm));

        AtsMatchResult result = scoringService.computeAtsScore(candidate, job);

        assertNotNull(result);
        assertEquals(88, result.getMatchScore());
        assertEquals(4, result.getMatchingSkills().size());
        assertEquals(1, result.getMissingSkills().size());
        assertEquals(80.0, result.getSkillMatchPercentage());
        assertEquals(100.0, result.getExperienceMatchPercentage());
    }

    @Test
    void testComputeScore_WithRuleBasedFallback_DecomposesAndResolvesAliases() {
        when(llmAtsClient.scoreWithLlm(any(), any())).thenReturn(Optional.empty());

        AtsMatchResult result = scoringService.computeAtsScore(candidate, job);

        assertNotNull(result);
        assertTrue(result.getMatchScore() >= 75, "Intelligent fallback should score high for decomposed and alias skills");
        assertTrue(result.getMatchingSkills().contains("Java programming language"), "Java (Core Java, Java 8+) should match Java programming language");
        assertTrue(result.getMatchingSkills().contains("Cloud computing platforms (e.g. AWS)"), "AWS (EC2) should match Cloud computing platforms (e.g. AWS)");
        assertTrue(result.getMatchingSkills().contains("Data structures and algorithms"), "DSA should match Data structures and algorithms");
        assertTrue(result.getMissingSkills().contains("MongoDB"), "MongoDB must be reported missing");
    }

    @Test
    void testComputeScore_WithRuleBasedFallback_NoFalsePositives() {
        when(llmAtsClient.scoreWithLlm(any(), any())).thenReturn(Optional.empty());

        Candidate jsOnlyCandidate = Candidate.builder()
                .id(UUID.randomUUID())
                .candidateId("CAND-002")
                .skills(Set.of(Skill.builder().name("JavaScript").build(), Skill.builder().name("NoSQL").build()))
                .build();

        Job javaSqlJob = Job.builder()
                .id(UUID.randomUUID())
                .title("Java SQL Developer")
                .skills(List.of("Java", "SQL"))
                .build();

        AtsMatchResult result = scoringService.computeAtsScore(jsOnlyCandidate, javaSqlJob);

        assertNotNull(result);
        assertFalse(result.getMatchingSkills().contains("Java"), "JavaScript must not match Java");
        assertFalse(result.getMatchingSkills().contains("SQL"), "NoSQL must not match SQL");
        assertEquals(2, result.getMissingSkills().size());
    }

    @Test
    void testComputeScore_RealExample_MatchesConceptsAndRecognizesGaps() {
        when(llmAtsClient.scoreWithLlm(any(), any())).thenReturn(Optional.empty());

        List<String> resumeSkillList = List.of(
                "Core Java", "OOP", "Collections", "Multithreading", "Spring Boot",
                "Spring MVC", "Spring Security", "Spring Data JPA", "Hibernate",
                "REST APIs", "JWT", "OAuth", "MySQL", "PostgreSQL", "React.js",
                "JUnit", "Mockito", "Git", "GitHub", "Maven", "Postman", "AWS (EC2)",
                "DSA", "DBMS", "OS", "Microservices", "Design Patterns", "SDLC", "Agile Methodologies"
        );

        java.util.Set<Skill> skillEntities = new java.util.HashSet<>();
        for (String s : resumeSkillList) {
            skillEntities.add(Skill.builder().id(UUID.randomUUID()).name(s).build());
        }

        Candidate fullCandidate = Candidate.builder()
                .id(UUID.randomUUID())
                .candidateId("CAND-MOHIT")
                .firstName("Mohit")
                .lastName("Kumar")
                .skills(skillEntities)
                .experience(BigDecimal.valueOf(3.5))
                .currentDesignation("Java Developer")
                .build();

        Job realisticJob = Job.builder()
                .id(UUID.randomUUID())
                .title("Java Backend Developer")
                .skills(List.of(
                        "Java programming language",
                        "Cloud computing platforms (e.g. AWS)",
                        "Java-based testing frameworks (e.g. JUnit)",
                        "Object-Oriented Programming (OOP) concepts",
                        "Data structures and algorithms",
                        "Database management systems (e.g. MySQL, MongoDB)",
                        "Version control systems (e.g. Git, SVN), Spring Framework experience, Azure",
                        "Agile development methodologies (e.g. Scrum, Kanban, TestNG)"
                ))
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .build();

        AtsMatchResult result = scoringService.computeAtsScore(fullCandidate, realisticJob);

        assertNotNull(result);
        assertTrue(result.getMatchScore() >= 75, "Expected high ATS score for real candidate resume skills, got: " + result.getMatchScore());

        boolean hasMissingAzureOrSvn = result.getMissingSkills().stream()
                .anyMatch(s -> s.toLowerCase().contains("azure") || s.toLowerCase().contains("svn"));
        assertTrue(hasMissingAzureOrSvn, "Azure and SVN must remain gaps when candidate does not possess them");
    }

    @Test
    void testRepairAndCleanJobSkills_ReunitesBrokenParentheses() {
        List<String> broken = List.of(
                "Java",
                "Database management systems (e.g. MySQL",
                "MongoDB)",
                "Version control systems (e.g. Git",
                "SVN)"
        );

        List<String> cleaned = AtsMatchScoringService.repairAndCleanJobSkills(broken);
        assertEquals(3, cleaned.size());
        assertEquals("Java", cleaned.get(0));
        assertEquals("Database management systems (e.g. MySQL, MongoDB)", cleaned.get(1));
        assertEquals("Version control systems (e.g. Git, SVN)", cleaned.get(2));
    }

    @Test
    void testComputeScore_RequiredVsPreferredWeighting_MissingPreferredNotHardGap() {
        when(llmAtsClient.scoreWithLlm(any(), any())).thenReturn(Optional.empty());

        Skill java = Skill.builder().name("Java").build();
        Skill spring = Skill.builder().name("Spring Boot").build();
        Skill sql = Skill.builder().name("SQL").build();

        Candidate candidateWithRequiredOnly = Candidate.builder()
                .id(UUID.randomUUID())
                .candidateId("CAND-REQ-ONLY")
                .skills(Set.of(java, spring, sql))
                .experience(BigDecimal.valueOf(3.0))
                .currentDesignation("Software Engineer")
                .build();

        Job jobWithPreferred = Job.builder()
                .id(UUID.randomUUID())
                .title("Software Engineer")
                .skills(List.of("Java", "Spring Boot", "SQL"))
                .preferredSkills(List.of("Docker", "AWS", "Kafka"))
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .build();

        AtsMatchResult result = scoringService.computeAtsScore(candidateWithRequiredOnly, jobWithPreferred);

        assertNotNull(result);
        assertFalse(result.getMissingSkills().contains("Docker"));
        assertFalse(result.getMissingSkills().contains("AWS"));
        assertFalse(result.getMissingSkills().contains("Kafka"));
        assertEquals(0, result.getMissingSkills().size(), "Candidate has all required skills, so missingSkills should be empty");
        assertEquals(80.0, result.getSkillMatchPercentage(), 0.01);
    }
}
