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
        String title = request.getJobTitle() != null && !request.getJobTitle().isBlank() ? request.getJobTitle() : "Software Engineer";
        String lowerTitle = title.toLowerCase();
        String expRaw = request.getExperienceLevel() != null && !request.getExperienceLevel().isBlank() ? request.getExperienceLevel().toLowerCase() : "mid";

        String normLevel;
        if (expRaw.contains("fresher") || expRaw.contains("entry") || expRaw.contains("junior")) {
            normLevel = "entry";
        } else if (expRaw.contains("lead") || expRaw.contains("staff") || expRaw.contains("principal")) {
            normLevel = "lead";
        } else if (expRaw.contains("executive") || expRaw.contains("director") || expRaw.contains("vp") || expRaw.contains("head")) {
            normLevel = "executive";
        } else if (expRaw.contains("senior") || expRaw.contains("sr")) {
            normLevel = "senior";
        } else {
            normLevel = "mid";
        }

        List<String> mustHave;
        List<String> niceToHave;
        List<String> softSkills;
        String education;
        java.util.Map<String, Double> expYears = new java.util.HashMap<>();

        if (request.getRequiredSkills() != null && !request.getRequiredSkills().isEmpty()) {
            mustHave = new java.util.ArrayList<>(request.getRequiredSkills());
            niceToHave = List.of("Git", "Agile Methodologies", "Problem Solving", "CI/CD", "Docker");
        } else if (lowerTitle.contains("python") || lowerTitle.contains("django") || lowerTitle.contains("fastapi") || lowerTitle.contains("flask")) {
            if ("entry".equals(normLevel)) {
                mustHave = List.of("Python", "Object-Oriented Programming", "Data Structures and Algorithms", "SQL", "FastAPI", "REST APIs", "PyTest", "Git", "Postman", "Linux");
                niceToHave = List.of("Django", "PostgreSQL", "Docker", "Redis", "Virtualenv", "Agile Methodologies");
            } else if ("senior".equals(normLevel)) {
                mustHave = List.of("Python", "FastAPI", "Microservices", "Distributed Systems", "System Design", "PostgreSQL", "Redis", "Celery", "Kafka", "Docker", "AWS", "CI/CD", "Mentoring");
                niceToHave = List.of("Kubernetes", "SQLAlchemy", "Asynchronous Programming", "NoSQL", "Database Optimization", "Observability");
            } else if ("lead".equals(normLevel)) {
                mustHave = List.of("Python", "FastAPI", "System Design", "Distributed Systems", "Architecture Ownership", "Team Leadership", "Microservices", "PostgreSQL", "Kafka", "Code Review", "Delivery Management", "Mentoring");
                niceToHave = List.of("AWS", "Kubernetes", "Technical Strategy", "CI/CD", "Observability", "Agile Coaching");
            } else if ("executive".equals(normLevel)) {
                mustHave = List.of("Technology Strategy", "Engineering Leadership", "System Architecture", "Roadmap Planning", "Technology Governance", "Hiring", "Budgeting", "Stakeholder Management", "Enterprise Security", "Vendor Management");
                niceToHave = List.of("Cloud Strategy", "Data Governance", "Operational Excellence", "Technical Due Diligence", "Scalability Strategy");
            } else {
                mustHave = List.of("Python", "FastAPI", "Django", "PostgreSQL", "SQLAlchemy", "REST APIs", "PyTest", "Docker", "Redis", "Git", "Microservices", "CI/CD");
                niceToHave = List.of("Celery", "Kafka", "AWS", "Linux", "Performance Profiling", "Asynchronous Programming");
            }
        } else if (lowerTitle.contains("front") || lowerTitle.contains("react") || lowerTitle.contains("vue") || lowerTitle.contains("angular") || lowerTitle.contains("ui")) {
            if ("entry".equals(normLevel)) {
                mustHave = List.of("JavaScript", "React", "HTML5", "CSS3", "Data Structures and Algorithms", "REST APIs", "Git", "Responsive Web Design", "Postman", "Browser Developer Tools");
                niceToHave = List.of("TypeScript", "Tailwind CSS", "Jest", "Webpack", "Vite", "Figma");
            } else if ("senior".equals(normLevel)) {
                mustHave = List.of("JavaScript", "TypeScript", "React", "Next.js", "State Management", "Frontend Architecture", "Web Performance", "Core Web Vitals", "Testing Library", "CI/CD", "Design Systems", "REST APIs", "Mentoring");
                niceToHave = List.of("GraphQL", "Micro Frontends", "WebSockets", "Accessibility", "Docker", "Tailwind CSS");
            } else if ("lead".equals(normLevel)) {
                mustHave = List.of("JavaScript", "TypeScript", "React", "Frontend Architecture", "Design Systems", "Team Leadership", "Web Performance", "Code Review", "Delivery Management", "Mentoring", "Testing Strategy", "Technical Strategy");
                niceToHave = List.of("Micro Frontends", "CI/CD", "Next.js", "Accessibility", "Cross-Browser Compatibility", "Stakeholder Management");
            } else if ("executive".equals(normLevel)) {
                mustHave = List.of("Technology Strategy", "Engineering Leadership", "Frontend Platform Strategy", "User Experience Strategy", "Technology Governance", "Hiring", "Budgeting", "Stakeholder Management", "Vendor Management", "Engineering Culture");
                niceToHave = List.of("Product Alignment", "Digital Transformation", "Web Standards", "Operational Excellence", "Performance Governance");
            } else {
                mustHave = List.of("JavaScript", "TypeScript", "React", "HTML5", "CSS3", "Tailwind CSS", "State Management", "REST APIs", "Jest", "Vite", "Git", "Web Performance");
                niceToHave = List.of("Next.js", "Redux", "GraphQL", "CI/CD", "Responsive Web Design", "Accessibility");
            }
        } else if (lowerTitle.contains("full stack") || lowerTitle.contains("fullstack")) {
            if ("entry".equals(normLevel)) {
                mustHave = List.of("Java", "JavaScript", "React", "HTML5", "CSS3", "SQL", "Spring Boot", "REST APIs", "Git", "Data Structures and Algorithms");
                niceToHave = List.of("TypeScript", "JUnit", "Postman", "Maven", "Tailwind CSS", "Docker");
            } else if ("senior".equals(normLevel)) {
                mustHave = List.of("Java", "Spring Boot", "React", "TypeScript", "System Design", "Microservices", "Distributed Systems", "PostgreSQL", "State Management", "Docker", "AWS", "CI/CD", "Mentoring");
                niceToHave = List.of("Redis", "Kafka", "Kubernetes", "GraphQL", "Next.js", "Web Performance", "Testing Strategies");
            } else if ("lead".equals(normLevel)) {
                mustHave = List.of("Java", "React", "TypeScript", "System Design", "Full Stack Architecture", "Team Leadership", "Microservices", "PostgreSQL", "Code Review", "Delivery Management", "Mentoring", "Architecture Ownership");
                niceToHave = List.of("Cloud Architecture", "CI/CD", "Kafka", "Technical Strategy", "Observability", "Agile Coaching");
            } else if ("executive".equals(normLevel)) {
                mustHave = List.of("Technology Strategy", "Engineering Leadership", "System Architecture", "Roadmap Planning", "Technology Governance", "Hiring", "Budgeting", "Stakeholder Management", "Vendor Management", "Enterprise Security");
                niceToHave = List.of("Cloud Governance", "Product Strategy", "Engineering Culture", "Operational Excellence", "Scalability Strategy");
            } else {
                mustHave = List.of("Java", "React", "TypeScript", "Spring Boot", "Hibernate", "PostgreSQL", "REST APIs", "Tailwind CSS", "Redux", "Docker", "Git", "Jest");
                niceToHave = List.of("Microservices", "CI/CD", "Redis", "AWS", "JUnit", "Agile Methodologies");
            }
        } else if (lowerTitle.contains("devops") || lowerTitle.contains("cloud") || lowerTitle.contains("sre")) {
            if ("entry".equals(normLevel)) {
                mustHave = List.of("Linux", "Bash Scripting", "Git", "Python", "Networking Basics", "Docker", "CI/CD Basics", "Cloud Fundamentals", "Virtualization", "Postman");
                niceToHave = List.of("AWS", "Terraform", "GitHub Actions", "Nginx", "YAML", "Monitoring Basics");
            } else if ("senior".equals(normLevel)) {
                mustHave = List.of("Kubernetes", "Cloud Architecture", "AWS", "Terraform", "Infrastructure as Code", "CI/CD", "Prometheus", "Grafana", "Observability", "Security and Compliance", "Distributed Systems", "System Design", "Mentoring");
                niceToHave = List.of("Helm", "GitOps", "Cost Optimization", "Disaster Recovery", "Linux", "Azure", "Site Reliability Engineering");
            } else {
                mustHave = List.of("Linux", "Docker", "Kubernetes", "AWS", "Terraform", "CI/CD", "Bash Scripting", "Python", "Git", "Prometheus", "Grafana", "Infrastructure as Code");
                niceToHave = List.of("Ansible", "Helm", "GitHub Actions", "GCP", "Azure", "Security Best Practices");
            }
        } else if (lowerTitle.contains("qa") || lowerTitle.contains("test") || lowerTitle.contains("sdet")) {
            if ("entry".equals(normLevel)) {
                mustHave = List.of("Software Testing Fundamentals", "Test Case Design", "Java", "Selenium WebDriver", "Test Execution", "Bug Tracking", "JIRA", "SQL", "Git", "Postman");
                niceToHave = List.of("TestNG", "JUnit", "Python", "API Testing", "Agile Methodologies", "CI/CD Basics");
            } else if ("senior".equals(normLevel)) {
                mustHave = List.of("Test Automation Architecture", "Framework Design", "Selenium WebDriver", "Playwright", "API Testing", "REST Assured", "CI/CD Integration", "Performance Testing", "JMeter", "SQL", "Test Strategy", "Git", "Mentoring");
                niceToHave = List.of("Cypress", "Docker", "Security Testing", "Cross-Browser Testing", "TestNG", "Agile Methodologies");
            } else {
                mustHave = List.of("Test Automation", "Selenium WebDriver", "Java", "TestNG", "API Testing", "Postman", "REST Assured", "SQL", "JIRA", "Git", "CI/CD", "Test Execution");
                niceToHave = List.of("Cypress", "Playwright", "Cucumber", "Python", "Docker", "Performance Testing Basics");
            }
        } else {
            // Java / Backend default
            if ("entry".equals(normLevel)) {
                mustHave = List.of("Java", "Object-Oriented Programming", "Data Structures and Algorithms", "SQL", "Spring Boot", "REST APIs", "JUnit", "Maven", "Git", "Postman");
                niceToHave = List.of("Docker", "Linux", "Hibernate", "Agile Methodologies", "Mockito", "AWS");
            } else if ("senior".equals(normLevel)) {
                mustHave = List.of("Java", "Spring Boot", "Microservices", "Distributed Systems", "System Design", "PostgreSQL", "Redis", "Kafka", "Spring Security", "Docker", "Kubernetes", "AWS", "Mentoring");
                niceToHave = List.of("CI/CD", "Observability", "NoSQL", "Performance Optimization", "Database Tuning", "Technical Leadership");
            } else if ("lead".equals(normLevel)) {
                mustHave = List.of("Java", "Spring Boot", "System Design", "Distributed Systems", "Architecture Ownership", "Team Leadership", "Microservices", "PostgreSQL", "Kafka", "Code Review", "Delivery Management", "Mentoring");
                niceToHave = List.of("AWS", "Kubernetes", "Technical Strategy", "CI/CD", "Observability", "Agile Coaching");
            } else if ("executive".equals(normLevel)) {
                mustHave = List.of("Technology Strategy", "Engineering Leadership", "Technology Governance", "System Architecture", "Budgeting", "Hiring", "Vendor Management", "Enterprise Security", "Roadmap Planning", "Stakeholder Management");
                niceToHave = List.of("Cloud Strategy", "Distributed Systems", "Software Quality Governance", "Digital Transformation", "Risk Management");
            } else {
                mustHave = List.of("Java", "Spring Boot", "Spring Security", "Hibernate", "PostgreSQL", "REST APIs", "Microservices", "JUnit", "Mockito", "Docker", "Git", "CI/CD");
                niceToHave = List.of("Redis", "Kafka", "AWS", "Linux", "Performance Tuning", "Agile Methodologies");
            }
        }

        switch (normLevel) {
            case "entry" -> {
                expYears.put("min", 0.0);
                expYears.put("max", 2.0);
                education = "Bachelor's degree in Computer Science, Information Technology, or equivalent practical background";
                softSkills = List.of("Problem Solving", "Analytical Thinking", "Team Collaboration", "Willingness to Learn");
            }
            case "senior" -> {
                expYears.put("min", 5.0);
                expYears.put("max", 8.0);
                education = "Bachelor's or Master's degree in Computer Science, Software Engineering, or equivalent experience";
                softSkills = List.of("Technical Mentorship", "System Architecture Trade-Offs", "Cross-Functional Collaboration", "Root Cause Analysis");
            }
            case "lead" -> {
                expYears.put("min", 8.0);
                expYears.put("max", 12.0);
                education = "Bachelor's or Master's degree with demonstrable engineering leadership track record";
                softSkills = List.of("Team Leadership", "Delivery Management", "Engineering Mentorship", "Stakeholder Communication");
            }
            case "executive" -> {
                expYears.put("min", 10.0);
                expYears.put("max", 20.0);
                education = "Bachelor's or Master's degree with executive engineering credentials";
                softSkills = List.of("Strategic Leadership", "Enterprise Governance", "Executive Alignment", "High-Performance Culture Building");
            }
            default -> {
                expYears.put("min", 2.0);
                expYears.put("max", 5.0);
                education = "Bachelor's degree in Computer Science, Information Systems, or related field";
                softSkills = List.of("Collaborative Problem Solving", "Code Quality Ownership", "Effective Technical Communication");
            }
        }

        List<String> responsibilities = List.of(
                "Design, build, and maintain efficient, scalable, and reliable services for the " + title + " role.",
                "Collaborate with cross-functional teams to define, architect, and deliver high-impact product features.",
                "Write clean, well-tested, and maintainable code adhering to industry best practices.",
                "Identify bottlenecks and bugs, and devise proactive solutions to optimize system performance."
        );

        List<String> interviewQuestions = List.of(
                "Describe your core hands-on experience and architecture choices in " + title + " projects.",
                "How do you approach debugging, performance profiling, and error handling in high-throughput applications?",
                "Can you walk us through a complex technical challenge you solved recently?"
        );

        return AiJdGenerateResponse.builder()
                .description("We are looking for a skilled and motivated " + title + " (" + normLevel.toUpperCase() + ") to join our engineering division.")
                .responsibilities(responsibilities)
                .mustHaveSkills(mustHave)
                .niceToHaveSkills(niceToHave)
                .required(mustHave)
                .preferred(niceToHave)
                .experienceYears(expYears)
                .education(education)
                .softSkills(softSkills)
                .roleFamily(lowerTitle)
                .interviewQuestions(interviewQuestions)
                .build();
    }
}
