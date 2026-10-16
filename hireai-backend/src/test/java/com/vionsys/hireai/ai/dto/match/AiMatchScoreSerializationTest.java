package com.vionsys.hireai.ai.dto.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class AiMatchScoreSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testAiMatchScoreResponse_DeserializationWithNewAndUnknownFields() throws Exception {
        String json = """
        {
            "matchScore": 88,
            "autoAction": "shortlist",
            "matchedSkills": ["Java programming language", "Cloud computing platforms (e.g. AWS)"],
            "missingSkills": ["MongoDB"],
            "bonusSkills": ["Spring Boot", "Docker"],
            "matchDetails": [
                {
                    "requiredSkill": "Cloud computing platforms (e.g. AWS)",
                    "status": "matched",
                    "matchedWith": "AWS (EC2)",
                    "matchType": "example",
                    "confidence": 1.0
                }
            ],
            "skillCoveragePercent": 87.5,
            "experienceMatchPercent": 100.0,
            "scoreBreakdown": {
                "skillScore": 88,
                "experienceScore": 100
            },
            "rationale": "Matched 7 of 8 required skills. Missing: MongoDB.",
            "engineVersion": "match-v2",
            "semanticModelAvailable": true,
            "futureUnknownField": "should be safely ignored without breaking"
        }
        """;

        AiMatchScoreResponse response = objectMapper.readValue(json, AiMatchScoreResponse.class);

        assertNotNull(response);
        assertEquals(88, response.getMatchScore());
        assertEquals("shortlist", response.getAutoAction());
        assertEquals(2, response.getMatchedSkills().size());
        assertEquals(List.of("MongoDB"), response.getMissingSkills());
        assertEquals(List.of("Spring Boot", "Docker"), response.getBonusSkills());
        assertEquals(87.5, response.getSkillCoveragePercent());
        assertEquals(100.0, response.getExperienceMatchPercent());
        assertEquals("match-v2", response.getEngineVersion());
        assertTrue(response.getSemanticModelAvailable());
        assertNotNull(response.getScoreBreakdown());
        assertEquals(88, response.getScoreBreakdown().getSkillScore());
        assertEquals(1, response.getMatchDetails().size());
        assertEquals("example", response.getMatchDetails().get(0).getMatchType());
    }
}
