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
    private final com.vionsys.hireai.application.config.AtsProperties atsProperties;

    private static final Set<String> SPECIFIC_TOOLS = Set.of(
            "docker", "kubernetes", "k8s", "typescript", "ts", "javascript", "js",
            "python", "py", "java", "c++", "c#", ".net", "golang", "go", "rust",
            "mysql", "postgresql", "postgres", "mongodb", "redis", "cassandra",
            "oracle", "sql server", "spring boot", "hibernate", "react", "angular",
            "vue", "node.js", "aws", "azure", "gcp", "git", "github", "gitlab",
            "junit", "mockito", "kafka", "rabbitmq"
    );

    private static final Map<String, Set<String>> RELATED_SKILLS = Map.of(
            "typescript", Set.of("javascript", "js"),
            "javascript", Set.of("typescript", "ts"),
            "azure", Set.of("aws", "gcp"),
            "aws", Set.of("azure", "gcp"),
            "docker", Set.of("podman", "containerd"),
            "kubernetes", Set.of("docker swarm", "openshift")
    );

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

    public enum MatchStatus {
        MATCHED, RELATED, MISSING
    }

    public static class SkillEvaluation {
        public final MatchStatus status;
        public final String matchedWith;
        public final double credit;

        public SkillEvaluation(MatchStatus status, String matchedWith, double credit) {
            this.status = status;
            this.matchedWith = matchedWith;
            this.credit = credit;
        }
    }

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
                    .relatedSkills(res.getRelatedSkills() != null ? res.getRelatedSkills() : Collections.emptyList())
                    .skillMatchPercentage(skillPercent)
                    .experienceMatchPercentage(expPercent)
                    .build();
        }

        // 2. Rule-Based Intelligent Matching Engine (Fallback)
        List<String> rawJobSkills = job.getSkills() != null
                ? job.getSkills().stream().map(String::trim).filter(s -> !s.isBlank()).toList()
                : Collections.emptyList();
        List<String> jobSkills = repairAndCleanJobSkills(rawJobSkills);

        List<String> rawPrefSkills = job.getPreferredSkills() != null
                ? job.getPreferredSkills().stream().map(String::trim).filter(s -> !s.isBlank()).toList()
                : Collections.emptyList();
        List<String> prefSkills = repairAndCleanJobSkills(rawPrefSkills);

        List<String> candidateSkillNames = new ArrayList<>();
        if (candidate.getSkills() != null) {
            for (Skill s : candidate.getSkills()) {
                if (s.getName() != null && !s.getName().isBlank()) {
                    candidateSkillNames.add(s.getName().trim());
                }
            }
        }

        String rawResumeText = candidate.getResume() != null ? candidate.getResume().getRawText() : null;

        // Ground candidate skills against resume text if provided
        List<String> groundedSkills = new ArrayList<>();
        if (rawResumeText != null && !rawResumeText.isBlank()) {
            for (String c : candidateSkillNames) {
                if (tokenMatches(c, rawResumeText) || tokenMatches(normalize(c), rawResumeText)) {
                    groundedSkills.add(c);
                }
            }
            // Add any deterministically recognized skills directly from resume text
            for (Map.Entry<String, String> entry : COMMON_ALIASES.entrySet()) {
                if (tokenMatches(entry.getKey(), rawResumeText) && !groundedSkills.contains(entry.getValue())) {
                    groundedSkills.add(entry.getValue());
                }
            }
        } else {
            groundedSkills.addAll(candidateSkillNames);
        }

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        List<String> relatedSkills = new ArrayList<>();
        double totalMatchedUnits = 0.0;
        double totalRequiredUnits = 0.0;

        // Evaluate Required Skills
        if (jobSkills.isEmpty()) {
            matchedSkills.addAll(groundedSkills);
        } else {
            for (String required : jobSkills) {
                List<String> subItems = splitOutsideParens(required);
                for (String sub : subItems) {
                    totalRequiredUnits += 1.0;
                    SkillEvaluation eval = evaluateSkill(sub, groundedSkills, rawResumeText);
                    if (eval.status == MatchStatus.MATCHED) {
                        matchedSkills.add(sub);
                        totalMatchedUnits += eval.credit;
                    } else if (eval.status == MatchStatus.RELATED) {
                        missingSkills.add(sub);
                        relatedSkills.add(sub + " (via " + eval.matchedWith + ", " + (int)(eval.credit * 100) + "% credit)");
                        totalMatchedUnits += eval.credit;
                    } else {
                        missingSkills.add(sub);
                    }
                }
            }
        }

        // Evaluate Preferred Skills (Nice to have - missing preferred skills NEVER count as missing gaps)
        double totalPrefMatchedUnits = 0.0;
        double totalPrefUnits = 0.0;
        for (String pref : prefSkills) {
            List<String> subItems = splitOutsideParens(pref);
            for (String sub : subItems) {
                totalPrefUnits += 1.0;
                SkillEvaluation eval = evaluateSkill(sub, groundedSkills, rawResumeText);
                if (eval.status == MatchStatus.MATCHED) {
                    totalPrefMatchedUnits += eval.credit;
                    if (!matchedSkills.contains(sub)) {
                        matchedSkills.add(sub + " (Preferred)");
                    }
                } else if (eval.status == MatchStatus.RELATED) {
                    totalPrefMatchedUnits += eval.credit * 0.5;
                }
            }
        }

        double requiredSkillScore = totalRequiredUnits > 0 ? (totalMatchedUnits / totalRequiredUnits) * 100.0 : 100.0;
        double skillScore;
        if (totalPrefUnits > 0) {
            double preferredSkillScore = (totalPrefMatchedUnits / totalPrefUnits) * 100.0;
            // 80% required skills / 20% preferred skills
            skillScore = (requiredSkillScore * 0.80) + (preferredSkillScore * 0.20);
        } else {
            // Legacy flat list compatibility: 100% required
            skillScore = requiredSkillScore;
        }

        double expPercent = calculateExperiencePercent(candidate.getExperience(), job.getExperienceLevel());
        double titlePercent = calculateTitlePercent(candidate.getCurrentDesignation(), job.getTitle());

        double wReq = atsProperties.getWeightRequiredSkills() + atsProperties.getWeightPreferredSkills();
        double wExp = atsProperties.getWeightExperience();
        double wRole = atsProperties.getWeightRole();

        int finalScore = (int) Math.min(100, Math.round((skillScore * wReq) + (expPercent * wExp) + (titlePercent * wRole)));

        log.info("ATS score computed via rule-based intelligent fallback: {}% for Candidate {}",
                finalScore, candidate.getCandidateId());

        return AtsMatchResult.builder()
                .matchScore(finalScore)
                .matchingSkills(matchedSkills)
                .missingSkills(missingSkills)
                .relatedSkills(relatedSkills)
                .skillMatchPercentage(skillScore)
                .experienceMatchPercentage(expPercent)
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

    private SkillEvaluation evaluateSkill(String required, List<String> candidateSkills, String rawResumeText) {
        String reqNorm = normalize(required);
        boolean isSpecific = SPECIFIC_TOOLS.contains(reqNorm);

        // 1. Exact or alias match against candidate skill list
        for (String c : candidateSkills) {
            String cNorm = normalize(c);
            if (reqNorm.equals(cNorm)) {
                return new SkillEvaluation(MatchStatus.MATCHED, c, 1.0);
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
                            return new SkillEvaluation(MatchStatus.MATCHED, c, 1.0);
                        }
                    }
                    if (rawResumeText != null && tokenMatches(exTrim, rawResumeText)) {
                        return new SkillEvaluation(MatchStatus.MATCHED, exTrim, 1.0);
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
                            return new SkillEvaluation(MatchStatus.MATCHED, c, 1.0);
                        }
                    }
                    if (rawResumeText != null && tokenMatches(partTrim, rawResumeText)) {
                        return new SkillEvaluation(MatchStatus.MATCHED, partTrim, 1.0);
                    }
                }
            }
        }

        // 4. Category checks (ONLY if requirement is NOT a specific technology!)
        if (!isSpecific) {
            for (Map.Entry<String, Set<String>> cat : CATEGORY_MEMBERS.entrySet()) {
                if (reqNorm.contains(cat.getKey()) || required.toLowerCase().contains(cat.getKey())) {
                    for (String c : candidateSkills) {
                        String cNorm = normalize(c);
                        if (cat.getValue().contains(cNorm)) {
                            return new SkillEvaluation(MatchStatus.MATCHED, c, 1.0);
                        }
                    }
                    if (rawResumeText != null) {
                        for (String member : cat.getValue()) {
                            if (tokenMatches(member, rawResumeText)) {
                                return new SkillEvaluation(MatchStatus.MATCHED, member, 1.0);
                            }
                        }
                    }
                }
            }
        }

        // 5. Related but different technology (e.g. JavaScript for TypeScript)
        if (RELATED_SKILLS.containsKey(reqNorm)) {
            Set<String> relatedSet = RELATED_SKILLS.get(reqNorm);
            for (String c : candidateSkills) {
                String cNorm = normalize(c);
                if (relatedSet.contains(cNorm)) {
                    return new SkillEvaluation(MatchStatus.RELATED, c, atsProperties.getRelatedSkillCredit());
                }
            }
            if (rawResumeText != null) {
                for (String rel : relatedSet) {
                    if (tokenMatches(rel, rawResumeText)) {
                        return new SkillEvaluation(MatchStatus.RELATED, rel, atsProperties.getRelatedSkillCredit());
                    }
                }
            }
        }

        // 6. Direct token match only for non-specific items
        if (!isSpecific) {
            for (String c : candidateSkills) {
                if (tokenMatches(reqNorm, c) || tokenMatches(c, required)) {
                    return new SkillEvaluation(MatchStatus.MATCHED, c, 1.0);
                }
            }
            if (rawResumeText != null && tokenMatches(reqNorm, rawResumeText)) {
                return new SkillEvaluation(MatchStatus.MATCHED, reqNorm, 1.0);
            }
        }

        return new SkillEvaluation(MatchStatus.MISSING, null, 0.0);
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

        String escaped = Pattern.quote(tLow);
        Pattern p = Pattern.compile("(?<![a-zA-Z0-9_])" + escaped + "(?![a-zA-Z0-9_])");

        // Specific negative guard for SQL vs NoSQL: match only if sql exists outside of nosql
        if (tLow.equals("sql") && tarLow.contains("nosql")) {
            String withoutNosql = tarLow.replaceAll("(?i)\\bnosql\\b", " ");
            return p.matcher(withoutNosql).find();
        }

        // Specific negative guard for Go vs MongoDB / Algo / Django
        if (tLow.equals("go") && (tarLow.contains("mongodb") || tarLow.contains("django") || tarLow.contains("algo"))) {
            Matcher m = p.matcher(tarLow);
            while (m.find()) {
                int start = Math.max(0, m.start() - 5);
                int end = Math.min(tarLow.length(), m.end() + 5);
                String surrounding = tarLow.substring(start, end);
                if (!surrounding.contains("mongo") && !surrounding.contains("django") && !surrounding.contains("algo")) {
                    return true;
                }
            }
            return false;
        }

        return p.matcher(tarLow).find();
    }

    private double calculateExperiencePercent(BigDecimal candidateExp, ExperienceLevel requiredLevel) {
        if (requiredLevel == null) return 100.0;
        double minExpected = switch (requiredLevel) {
            case FRESHER -> 0.0;
            case JUNIOR -> 1.0;
            case MID_LEVEL -> 3.0;
            case SENIOR -> 5.0;
            case LEAD -> 8.0;
            case EXECUTIVE -> 10.0;
        };
        if (minExpected <= 0.0) return 100.0;
        if (candidateExp == null) return 50.0;
        double exp = candidateExp.doubleValue();
        return Math.min(100.0, Math.max(0.0, (exp / minExpected) * 100.0));
    }

    private double calculateTitlePercent(String candidateDesignation, String jobTitle) {
        if (candidateDesignation == null || jobTitle == null) {
            return 50.0;
        }
        String cLow = candidateDesignation.toLowerCase();
        String jLow = jobTitle.toLowerCase();
        if (cLow.equals(jLow)) return 100.0;
        String[] cTokens = cLow.split("\\s+");
        String[] jTokens = jLow.split("\\s+");
        int overlap = 0;
        for (String ct : cTokens) {
            for (String jt : jTokens) {
                if (ct.equals(jt) && ct.length() > 2) overlap++;
            }
        }
        return overlap > 0 ? Math.min(100.0, 70.0 + overlap * 15.0) : 60.0;
    }
}

