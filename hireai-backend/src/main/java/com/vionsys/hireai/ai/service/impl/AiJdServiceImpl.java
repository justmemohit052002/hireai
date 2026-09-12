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
        String exp = request.getExperienceLevel() != null && !request.getExperienceLevel().isBlank() ? request.getExperienceLevel() : "Mid-Level";

        List<String> mustHave;
        List<String> niceToHave;
        List<String> responsibilities;
        List<String> interviewQuestions;

        if (request.getRequiredSkills() != null && !request.getRequiredSkills().isEmpty()) {
            mustHave = request.getRequiredSkills();
            niceToHave = List.of("Git & Version Control", "Agile Methodologies", "Problem Solving");
        } else if (lowerTitle.contains("python") || lowerTitle.contains("django") || lowerTitle.contains("fastapi") || lowerTitle.contains("flask")) {
            mustHave = List.of("Python", "Django / FastAPI", "PostgreSQL / MySQL", "RESTful APIs", "SQLAlchemy / ORM");
            niceToHave = List.of("Docker", "Redis", "Celery", "PyTest", "Git");
        } else if (lowerTitle.contains("front") || lowerTitle.contains("react") || lowerTitle.contains("vue") || lowerTitle.contains("angular") || lowerTitle.contains("ui")) {
            mustHave = List.of("React.js", "TypeScript", "JavaScript (ES6+)", "HTML5 / CSS3", "REST APIs");
            niceToHave = List.of("TailwindCSS", "Redux / Zustand", "Jest / React Testing Library", "Git", "Webpack / Vite");
        } else if (lowerTitle.contains("data sci") || lowerTitle.contains("machine learn") || lowerTitle.contains("ml") || lowerTitle.contains("ai")) {
            mustHave = List.of("Python", "PyTorch / TensorFlow", "Scikit-Learn", "SQL", "Feature Engineering");
            niceToHave = List.of("Docker", "FastAPI", "MLOps", "Pandas & NumPy", "Git");
        } else if (lowerTitle.contains("data anal") || lowerTitle.contains("business anal") || lowerTitle.contains("bi ")) {
            mustHave = List.of("SQL", "Python", "Power BI / Tableau", "Excel (Advanced)", "Data Modeling");
            niceToHave = List.of("ETL Pipelines", "Statistical Analysis", "Dashboarding", "JIRA");
        } else if (lowerTitle.contains("devops") || lowerTitle.contains("cloud") || lowerTitle.contains("sre") || lowerTitle.contains("infra")) {
            mustHave = List.of("Kubernetes", "Docker", "AWS / GCP", "CI/CD Pipelines", "Terraform");
            niceToHave = List.of("Linux / Bash", "Prometheus & Grafana", "Helm", "Security Best Practices");
        } else if (lowerTitle.contains("qa") || lowerTitle.contains("test") || lowerTitle.contains("sdet") || lowerTitle.contains("quality")) {
            mustHave = List.of("Selenium / Cypress", "Test Automation Frameworks", "API Testing (Postman/RestAssured)", "Java / Python");
            niceToHave = List.of("CI/CD Integration", "JIRA", "SQL", "Performance Testing (JMeter)");
        } else if (lowerTitle.contains("java") || lowerTitle.contains("spring") || lowerTitle.contains("backend")) {
            mustHave = List.of("Java", "Spring Boot", "Microservices", "PostgreSQL / MySQL", "RESTful APIs");
            niceToHave = List.of("Docker", "Redis", "Kafka", "Git", "System Design");
        } else {
            mustHave = List.of("Core Programming", "Data Structures & Algorithms", "RESTful APIs", "Relational Databases", "Problem Solving");
            niceToHave = List.of("Git & Version Control", "Docker", "Agile / Scrum Methodologies", "CI/CD");
        }

        responsibilities = List.of(
                "Design, build, and maintain efficient, scalable, and reliable services for the " + title + " role.",
                "Collaborate with cross-functional teams to define, architect, and deliver high-impact product features.",
                "Write clean, well-tested, and maintainable code adhering to industry best practices.",
                "Identify bottlenecks and bugs, and devise proactive solutions to optimize system performance."
        );

        interviewQuestions = List.of(
                "Describe your core hands-on experience and architecture choices in " + title + " projects.",
                "How do you approach debugging, performance profiling, and error handling in high-throughput applications?",
                "Can you walk us through a complex technical challenge you solved recently?"
        );

        return AiJdGenerateResponse.builder()
                .description("We are looking for a skilled and motivated " + title + " (" + exp + ") to join our innovative engineering division.")
                .responsibilities(responsibilities)
                .mustHaveSkills(mustHave)
                .niceToHaveSkills(niceToHave)
                .interviewQuestions(interviewQuestions)
                .build();
    }
}
