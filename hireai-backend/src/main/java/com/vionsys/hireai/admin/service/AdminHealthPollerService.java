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

    @org.springframework.beans.factory.annotation.Value("${admin.health.poll-rate-ms:45000}")
    private long pollRateMs = 45000L;

    private final AtomicReference<AdminHealthResponse> latestSnapshot = new AtomicReference<>();
    private volatile Instant lastCheckedInstant = Instant.EPOCH;
    private volatile Instant lastSuccessfulHealthPollInstant = null;
    private final Instant backendStartedInstant = Instant.now();

    public record ModuleState(String status, Instant since) {}
    private final java.util.concurrent.ConcurrentHashMap<String, ModuleState> moduleStateMap = new java.util.concurrent.ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Pre-populate with fallback so first page load NEVER blocks on live health check
        latestSnapshot.set(createFallbackAiHealth());
        log.info("Initialized AdminHealthPollerService with poll rate {}ms. Performing background initial poll.", pollRateMs);
        java.util.concurrent.CompletableFuture.runAsync(this::pollHealth);
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
                lastSuccessfulHealthPollInstant = now;
                if (snapshot.getModules() == null) {
                    snapshot.setModules(createDefaultModules("HEALTHY"));
                }
            } else {
                snapshot = createFallbackAiHealth();
            }

            // Track module state changes and timestamps
            if (snapshot.getModules() != null) {
                for (ModuleHealthDto mod : snapshot.getModules()) {
                    String modId = mod.getId() != null ? mod.getId() : mod.getTaskType();
                    String currentStatus = mod.getStatus() != null ? mod.getStatus().toUpperCase() : "UNKNOWN";
                    moduleStateMap.compute(modId, (id, prev) -> {
                        if (prev == null || !prev.status().equalsIgnoreCase(currentStatus)) {
                            return new ModuleState(currentStatus, now);
                        }
                        return prev;
                    });
                }
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

    public Instant getLastSuccessfulHealthPollInstant() {
        return lastSuccessfulHealthPollInstant;
    }

    public Instant getBackendStartedInstant() {
        return backendStartedInstant;
    }

    public long getModuleStateDurationSeconds(String moduleId) {
        ModuleState state = moduleStateMap.get(moduleId);
        if (state == null || state.since() == null) {
            return java.time.Duration.between(backendStartedInstant, Instant.now()).toSeconds();
        }
        return Math.max(0, java.time.Duration.between(state.since(), Instant.now()).toSeconds());
    }

    public AdminHealthResponse getCachedHealthSnapshot() {
        AdminHealthResponse snapshot = latestSnapshot.get();
        if (snapshot == null) {
            snapshot = createFallbackAiHealth();
            latestSnapshot.set(snapshot);
        }

        // Check if snapshot is older than 3x the poll interval
        long effectivePollRateMs = this.pollRateMs > 0 ? this.pollRateMs : 45000L;
        boolean stale = lastCheckedInstant == null || Instant.now().isAfter(lastCheckedInstant.plusMillis(3 * effectivePollRateMs));

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
