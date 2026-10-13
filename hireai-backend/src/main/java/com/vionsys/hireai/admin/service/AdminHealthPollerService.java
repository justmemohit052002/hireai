package com.vionsys.hireai.admin.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.vionsys.hireai.admin.dto.AdminHealthResponse;
import com.vionsys.hireai.admin.dto.EnvironmentConfigDto;
import com.vionsys.hireai.admin.dto.ModuleHealthDto;
import com.vionsys.hireai.admin.dto.ProviderDetailsDto;
import com.vionsys.hireai.admin.dto.ProvidersConfigDto;
import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.ai.config.AiEngineProperties;
import com.vionsys.hireai.user.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminHealthPollerService {

    private final AiEngineClient aiEngineClient;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AiEngineProperties aiEngineProperties;

    private final AtomicReference<AdminHealthResponse> latestSnapshot = new AtomicReference<>();
    private volatile Instant lastCheckedInstant = Instant.EPOCH;

    @PostConstruct
    public void init() {
        log.info("Initializing AdminHealthPollerService background snapshot...");
        pollHealth();
    }

    @Scheduled(fixedRateString = "${admin.health.poll-rate-ms:45000}")
    public void pollHealth() {
        try {
            Instant now = Instant.now();
            String dbStatus = checkDatabaseLiveness();
            long activeUsers = 0;
            long totalUsers = 0;
            try {
                activeUsers = userRepository.countActiveUsers();
                totalUsers = userRepository.countTotalUsers();
            } catch (Exception ex) {
                log.warn("Failed to retrieve user counts for health snapshot: {}", ex.getMessage());
            }

            Optional<AdminHealthResponse> engineHealthOpt = aiEngineClient.getAdminHealthSummary();
            AdminHealthResponse snapshot;

            if (engineHealthOpt.isPresent()) {
                snapshot = engineHealthOpt.get();
                if (snapshot.getModules() == null) {
                    snapshot.setModules(createDefaultModules("HEALTHY"));
                }
            } else {
                snapshot = createFallbackAiHealth();
            }

            snapshot.setBackendStatus("UP");
            snapshot.setDatabaseStatus(dbStatus);
            snapshot.setActiveUsersCount(activeUsers);
            snapshot.setTotalUsersCount(totalUsers);
            snapshot.setLastCheckedAt(now.toString());
            snapshot.setIsStale(false);

            latestSnapshot.set(snapshot);
            lastCheckedInstant = now;
            log.debug("Admin health poll completed at {} (DB: {}, AI Engine: {})", now, dbStatus, snapshot.getOverallStatus());
        } catch (Exception ex) {
            log.error("Unexpected error during scheduled health poll: {}", ex.getMessage(), ex);
        }
    }

    public AdminHealthResponse getCachedHealthSnapshot() {
        AdminHealthResponse snapshot = latestSnapshot.get();
        if (snapshot == null) {
            pollHealth();
            snapshot = latestSnapshot.get();
        }

        if (snapshot == null) {
            return createFallbackAiHealth();
        }

        // Check if snapshot is older than 3x the poll interval
        long pollRateMs = aiEngineProperties.getHealthPollRateMs() > 0 ? aiEngineProperties.getHealthPollRateMs() : 45000;
        boolean stale = lastCheckedInstant == null || Instant.now().isAfter(lastCheckedInstant.plusMillis(3 * pollRateMs));

        // Create shallow copy to set stale flag without mutating cached ref
        return AdminHealthResponse.builder()
                .overallStatus(snapshot.getOverallStatus())
                .service(snapshot.getService())
                .checkedAt(snapshot.getCheckedAt())
                .lastCheckedAt(snapshot.getLastCheckedAt())
                .isStale(stale)
                .backendStatus(snapshot.getBackendStatus())
                .databaseStatus(snapshot.getDatabaseStatus())
                .activeUsersCount(snapshot.getActiveUsersCount())
                .totalUsersCount(snapshot.getTotalUsersCount())
                .modules(snapshot.getModules())
                .providers(snapshot.getProviders())
                .environment(snapshot.getEnvironment())
                .error(snapshot.getError())
                .build();
    }

    private String checkDatabaseLiveness() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return (result != null && result == 1) ? "UP" : "DOWN";
        } catch (Exception ex) {
            log.warn("Database liveness check failed: {}", ex.getMessage());
            return "DOWN";
        }
    }

    private AdminHealthResponse createFallbackAiHealth() {
        return AdminHealthResponse.builder()
                .service("HireAI AI Engine")
                .overallStatus("OFFLINE")
                .checkedAt(Instant.now().toString())
                .lastCheckedAt(Instant.now().toString())
                .isStale(false)
                .backendStatus("UP")
                .databaseStatus("UP")
                .activeUsersCount(0L)
                .totalUsersCount(0L)
                .error("AI Engine unreachable")
                .modules(createDefaultModules("OFFLINE"))
                .providers(ProvidersConfigDto.builder()
                        .activeProvider("ollama")
                        .activeModel("llama3.2:3b")
                        .ollama(ProviderDetailsDto.builder().name("Ollama (Local)").isConfigured(true).reachable(false).build())
                        .gemini(ProviderDetailsDto.builder().name("Google Gemini").isConfigured(false).keyMasked("Not Configured").build())
                        .groq(ProviderDetailsDto.builder().name("Groq API").isConfigured(false).keyMasked("Not Configured").build())
                        .build())
                .environment(EnvironmentConfigDto.builder()
                        .appEnv("development")
                        .engineHost("localhost")
                        .enginePort(8000)
                        .build())
                .build();
    }

    private List<ModuleHealthDto> createDefaultModules(String status) {
        return List.of(
                ModuleHealthDto.builder().id("jd_generator").name("Job Description Generator").taskType("jd_generation").status(status).build(),
                ModuleHealthDto.builder().id("resume_parser").name("Resume Parser").taskType("resume_parsing").status(status).build(),
                ModuleHealthDto.builder().id("match_engine").name("Match Engine").taskType("match_scoring").status(status).build(),
                ModuleHealthDto.builder().id("chatbot").name("Pre-Screening Chatbot").taskType("chatbot_message").status(status).build(),
                ModuleHealthDto.builder().id("interview_ai").name("Interview AI").taskType("interview_ai").status(status).build(),
                ModuleHealthDto.builder().id("decision_engine").name("Decision Engine").taskType("decision_finalize").status(status).build()
        );
    }
}
