package com.vionsys.hireai.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.vionsys.hireai.application.dto.AtsMatchResult;
import com.vionsys.hireai.application.dto.llm.LlmAtsResponse;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.entity.Skill;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.enums.ExperienceLevel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtsMatchScoringService {

    private final LlmAtsClient llmAtsClient;

    private static final Map<String, String> COMMON_ALIASES = Map.ofEntries(
            Map.entry("dsa", "data structures and algorithms"),
            Map.entry("oop", "object-oriented programming"),
            Map.entry("oops", "object-oriented programming"),
            Map.entry("dbms", "database management systems"),
            Map.entry("rdbms", "database management systems"),
            Map.entry("k8s", "kubernetes"),
            Map.entry("js", "javascript"),
            Map.entry("ts", "typescript"),
            Map.entry("py", "python"),
            Map.entry("postgres", "postgresql"),
            Map.entry("mongo", "mongodb"),
            Map.entry("reactjs", "react"),
            Map.entry("nodejs", "node.js"),
            Map.entry("springboot", "spring boot"),
            Map.entry("aws", "amazon web services"),
            Map.entry("gcp", "google cloud platform"),
            Map.entry("os", "operating systems"),
            Map.entry("agile", "agile methodologies"),
            Map.entry("scrum", "agile methodologies"),
            Map.entry("kanban", "agile methodologies"),
            Map.entry("sdlc", "software development life cycle"),
            Map.entry("microservices", "microservice architecture"),
            Map.entry("design patterns", "software design patterns")
    );

    private static final Map<String, Set<String>> CATEGORY_MEMBERS = Map.of(
            "version control", Set.of("git", "github", "gitlab", "bitbucket", "svn"),
            "cloud computing", Set.of("aws", "azure", "gcp", "ec2", "s3", "lambda", "cloud"),
            "database management", Set.of("mysql", "postgresql", "postgres", "oracle", "sql server", "sqlite", "sql", "dbms", "rdbms", "mongodb"),
            "testing frameworks", Set.of("junit", "testng", "mockito", "pytest", "jest", "selenium"),
            "spring framework", Set.of("spring", "spring boot", "spring mvc", "spring security", "spring data jpa", "hibernate"),
            "agile", Set.of("agile", "scrum", "kanban", "sdlc", "jira"),
            "object-oriented", Set.of("oop", "oops", "object-oriented programming", "java", "c++", "c#", "python")
    );

    /**
     * Compute ATS match score (0-100) between a Candidate profile and a Job posting.
     * Uses AI Engine microservice if enabled/available, with graceful intelligent rule-based fallback.
     */
    public AtsMatchResult computeAtsScore(Candidate candidate, Job job) {

        // 1. Try external AI Engine scoring first
        Optional<LlmAtsResponse> llmResponse = llmAtsClient.scoreWithLlm(candidate, job);
        if (llmResponse.isPresent()) {
            LlmAtsResponse res = llmResponse.get();
            log.info("ATS score computed via AI Match Engine: {}% for Candidate {}",
                    res.getAtsScore(), candidate.getCandidateId());

            double skillPercent = res.getSkillCoveragePercent() != null
                    ? res.getSkillCoveragePercent()
                    : (double) res.getAtsScore();

            double expPercent = res.getExperienceMatchPercent() != null
                    ? res.getExperienceMatchPercent()
                    : (double) res.getAtsScore();

            return AtsMatchResult.builder()
                    .matchScore(Math.min(100, Math.max(0, res.getAtsScore())))
                    .matchingSkills(res.getMatchingSkills() != null ? res.getMatchingSkills() : Collections.emptyList())
                    .missingSkills(res.getMissingSkills() != null ? res.getMissingSkills() : Collections.emptyList())
                    .skillMatchPercentage(skillPercent)
                    .experienceMatchPercentage(expPercent)
                    .build();
        }

        // 2. Rule-Based Intelligent Matching Engine (Fallback)
        List<String> rawJobSkills = job.getSkills() != null
                ? job.getSkills().stream().map(String::trim).filter(s -> !s.isBlank()).toList()
                : Collections.emptyList();

        List<String> jobSkills = repairAndCleanJobSkills(rawJobSkills);

        List<String> candidateSkillNames = new ArrayList<>();
        if (candidate.getSkills() != null) {
            for (Skill s : candidate.getSkills()) {
                if (s.getName() != null && !s.getName().isBlank()) {
                    candidateSkillNames.add(s.getName().trim());
                }
            }
        }

        String rawResumeText = candidate.getResume() != null ? candidate.getResume().getRawText() : null;

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        double totalMatchedUnits = 0.0;
        double totalRequiredUnits = 0.0;

        if (jobSkills.isEmpty()) {
            matchedSkills.addAll(candidateSkillNames);
        } else {
            for (String required : jobSkills) {
                List<String> subItems = splitOutsideParens(required);
                if (subItems.size() > 1) {
                    // Bundled requirement (e.g. "Git, SVN, Spring Framework experience, Azure")
                    totalRequiredUnits += subItems.size();
                    for (String sub : subItems) {
                        if (isSkillMatched(sub, candidateSkillNames, rawResumeText)) {
                            matchedSkills.add(sub);
                            totalMatchedUnits += 1.0;
                        } else {
                            missingSkills.add(sub);
                        }
                    }
                } else {
                    totalRequiredUnits += 1.0;
                    if (isSkillMatched(required, candidateSkillNames, rawResumeText)) {
                        matchedSkills.add(required);
                        totalMatchedUnits += 1.0;
                    } else {
                        missingSkills.add(required);
                    }
                }
            }
        }

        double skillScore = 60.0;
        if (totalRequiredUnits > 0) {
            double skillRatio = totalMatchedUnits / totalRequiredUnits;
            skillScore = skillRatio * 60.0;
        }

        // Experience Level Matching (Weight: 25%)
        double expScore = calculateExperienceScore(candidate.getExperience(), job.getExperienceLevel());

        // Designation / Title Relevance (Weight: 15%)
        double titleScore = calculateTitleScore(candidate.getCurrentDesignation(), job.getTitle());

        int finalScore = (int) Math.min(100, Math.round(skillScore + expScore + titleScore));

        log.info("ATS score computed via rule-based intelligent fallback: {}% for Candidate {}",
                finalScore, candidate.getCandidateId());

        return AtsMatchResult.builder()
                .matchScore(finalScore)
                .matchingSkills(matchedSkills)
                .missingSkills(missingSkills)
                .skillMatchPercentage(totalRequiredUnits == 0 ? 100.0 : (totalMatchedUnits / totalRequiredUnits) * 100.0)
                .experienceMatchPercentage((expScore / 25.0) * 100.0)
                .build();
    }

    public static List<String> repairAndCleanJobSkills(List<String> rawSkills) {
        if (rawSkills == null || rawSkills.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> cleaned = new ArrayList<>();
        StringBuilder buffer = null;

        for (String item : rawSkills) {
            if (item == null) continue;
            String trimmed = item.trim();
            if (trimmed.isEmpty()) continue;

            if (buffer != null) {
                buffer.append(", ").append(trimmed);
                if (buffer.indexOf(")") != -1) {
                    cleaned.add(buffer.toString());
                    buffer = null;
                }
            } else if (trimmed.contains("(") && !trimmed.contains(")")) {
                buffer = new StringBuilder(trimmed);
            } else {
                cleaned.add(trimmed);
            }
        }
        if (buffer != null) {
            cleaned.add(buffer.toString() + ")");
        }
        return cleaned;
    }

    public static List<String> splitOutsideParens(String s) {
        if (s == null || s.isBlank()) {
            return Collections.emptyList();
        }
        String[] parts = s.split("[,;]\\s*(?![^()]*\\))");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result.isEmpty() ? List.of(s.trim()) : result;
    }

    private boolean isSkillMatched(String required, List<String> candidateSkills, String rawResumeText) {
        String reqNorm = normalize(required);

        // 1. Exact or alias match against candidate skill list
        for (String c : candidateSkills) {
            String cNorm = normalize(c);
            if (reqNorm.equals(cNorm)) {
                return true;
            }
        }

        // 2. Decompose parenthesized examples (e.g. "Cloud computing platforms (e.g. AWS)")
        Pattern egPattern = Pattern.compile("\\((?:e\\.g\\.?|eg:?|such as|including)\\s*([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = egPattern.matcher(required);
        if (matcher.find()) {
            String rawExamples = matcher.group(1);
            String[] examples = rawExamples.split("[,;/]|\\s+or\\s+");
            for (String ex : examples) {
                String exTrim = ex.trim();
                if (!exTrim.isBlank()) {
                    for (String c : candidateSkills) {
                        if (tokenMatches(exTrim, c) || normalize(exTrim).equals(normalize(c))) {
                            return true;
                        }
                    }
                    if (rawResumeText != null && tokenMatches(exTrim, rawResumeText)) {
                        return true;
                    }
                }
            }
        }

        // 3. Decompose alternatives (e.g. "A / B" or "A or B")
        if (required.contains("/") || required.matches(".*\\b(or|and/or)\\b.*")) {
            String[] parts = required.split("/|\\b(or|and/or)\\b");
            for (String part : parts) {
                String partTrim = part.trim();
                if (!partTrim.isBlank()) {
                    for (String c : candidateSkills) {
                        if (tokenMatches(partTrim, c) || normalize(partTrim).equals(normalize(c))) {
                            return true;
                        }
                    }
                    if (rawResumeText != null && tokenMatches(partTrim, rawResumeText)) {
                        return true;
                    }
                }
            }
        }

        // 4. Category checks
        for (Map.Entry<String, Set<String>> cat : CATEGORY_MEMBERS.entrySet()) {
            if (reqNorm.contains(cat.getKey())) {
                for (String c : candidateSkills) {
                    String cNorm = normalize(c);
                    if (cat.getValue().contains(cNorm)) {
                        return true;
                    }
                }
                if (rawResumeText != null) {
                    for (String member : cat.getValue()) {
                        if (tokenMatches(member, rawResumeText)) {
                            return true;
                        }
                    }
                }
            }
        }

        // 5. Strict token matching against candidate skills
        for (String c : candidateSkills) {
            if (tokenMatches(reqNorm, c) || tokenMatches(c, required)) {
                return true;
            }
        }

        // 6. Whole resume text search for base required skill or alias
        if (rawResumeText != null) {
            if (tokenMatches(reqNorm, rawResumeText)) {
                return true;
            }
            if (COMMON_ALIASES.containsKey(reqNorm) && tokenMatches(COMMON_ALIASES.get(reqNorm), rawResumeText)) {
                return true;
            }
            // Check reverse alias
            for (Map.Entry<String, String> entry : COMMON_ALIASES.entrySet()) {
                if (entry.getValue().equals(reqNorm) && tokenMatches(entry.getKey(), rawResumeText)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String normalize(String skill) {
        if (skill == null) return "";
        String s = skill.trim().toLowerCase();

        // Strip filler phrases
        s = s.replaceAll("^(?:proficient in|hands[- ]on experience with|knowledge of|expertise in)\\s+", "");
        s = s.replaceAll("\\s+(?:programming language|frameworks?|concepts?|tools?|platforms?|systems?)$", "");

        // Strip parenthetical notes like (core java, java 8+)
        s = s.replaceAll("\\s*\\([^)]*\\)", "").trim();

        // Check alias dictionary
        if (COMMON_ALIASES.containsKey(s)) {
            return COMMON_ALIASES.get(s);
        }

        return s;
    }

    private boolean tokenMatches(String token, String target) {
        if (token == null || target == null || token.isBlank() || target.isBlank()) return false;
        String tLow = token.trim().toLowerCase();
        String tarLow = target.trim().toLowerCase();

        // Negative guards against common false positives
        if (tLow.equals("java") && tarLow.contains("javascript")) return false;
        if (tLow.equals("sql") && tarLow.contains("nosql")) return false;
        if (tLow.equals("go") && tarLow.contains("mongodb")) return false;

        String escaped = Pattern.quote(tLow);
        Pattern p = Pattern.compile("(?<![a-zA-Z0-9_])" + escaped + "(?![a-zA-Z0-9_])");
        return p.matcher(tarLow).find();
    }

    private double calculateExperienceScore(BigDecimal candidateExp, ExperienceLevel requiredLevel) {
        if (candidateExp == null || requiredLevel == null) {
            return 15.0; // Moderate default
        }

        double exp = candidateExp.doubleValue();
        double minExpected = switch (requiredLevel) {
            case FRESHER -> 0.0;
            case JUNIOR -> 1.0;
            case MID_LEVEL -> 3.0;
            case SENIOR -> 5.0;
            case LEAD -> 8.0;
        };

        if (exp >= minExpected) {
            return 25.0;
        } else if (exp >= minExpected - 1.0) {
            return 18.0;
        } else {
            return Math.max(5.0, (exp / Math.max(1.0, minExpected)) * 25.0);
        }
    }

    private double calculateTitleScore(String candidateDesignation, String jobTitle) {
        if (candidateDesignation == null || jobTitle == null) {
            return 10.0;
        }

        String candLower = candidateDesignation.toLowerCase();
        String jobLower = jobTitle.toLowerCase();

        String[] jobWords = jobLower.split("\\s+");
        int matchedWords = 0;
        for (String word : jobWords) {
            if (word.length() > 2 && candLower.contains(word)) {
                matchedWords++;
            }
        }

        if (matchedWords > 0) {
            return 15.0;
        }
        return 7.0;
    }
}
