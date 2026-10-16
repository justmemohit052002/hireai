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
public class AdminHealthPollerService {

    private final AiEngineClient aiEngineClient;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public AdminHealthPollerService(AiEngineClient aiEngineClient, UserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.aiEngineClient = aiEngineClient;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @org.springframework.beans.factory.annotation.Value("${admin.health.poll-rate-ms:45000}")
    private long pollRateMs = 45000L;

    @org.springframework.beans.factory.annotation.Value("${admin.health.failure-threshold:2}")
    private int failureThreshold = 2;

    private final AtomicReference<AdminHealthResponse> latestSnapshot = new AtomicReference<>();
    private volatile Instant lastCheckedInstant = Instant.EPOCH;
    private volatile Instant lastSuccessfulHealthPollInstant = null;
    private volatile Instant lastAttemptInstant = null;
    private final java.util.concurrent.atomic.AtomicInteger consecutiveFailures = new java.util.concurrent.atomic.AtomicInteger(0);

    private volatile String lastReportedStatus = "INIT";
    private volatile String lastReportedReason = "";
    private volatile Instant lastWarnLoggedInstant = Instant.EPOCH;

    private final Instant backendStartedInstant = Instant.now();

    public record ModuleState(String status, Instant since) {}
    private final java.util.concurrent.ConcurrentHashMap<String, ModuleState> moduleStateMap = new java.util.concurrent.ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Pre-populate with fallback so first page load NEVER blocks on live health check
        latestSnapshot.set(createFallbackAiHealth("SERVICE_UNREACHABLE", "SERVICE_UNREACHABLE", "AI Engine initial health check pending"));
        log.info("Initialized AdminHealthPollerService with poll rate {}ms (failure threshold: {}). Performing background initial poll.",
                pollRateMs, failureThreshold);
        java.util.concurrent.CompletableFuture.runAsync(this::pollHealth);
    }

    @Scheduled(fixedRateString = "${admin.health.poll-rate-ms:45000}")
    public void pollHealth() {
        try {
            Instant now = Instant.now();
            lastAttemptInstant = now;
            String dbStatus = checkDatabaseLiveness();
            long activeUsers = 0;
            long totalUsers = 0;
            try {
                activeUsers = userRepository.countActiveUsers();
                totalUsers = userRepository.countTotalUsers();
            } catch (Exception ex) {
                log.warn("Failed to retrieve user counts for health snapshot: {}", ex.getMessage());
            }

            AiEngineClient.HealthCheckResult healthResult = aiEngineClient.checkAdminHealthDetailed();
            if (healthResult == null) {
                healthResult = new AiEngineClient.HealthCheckResult(false, null, "SERVICE_UNREACHABLE", "AI Engine health check returned no result", null);
            }
            AdminHealthResponse snapshot;

            if (healthResult.isSuccessful()) {
                int previousFailures = consecutiveFailures.getAndSet(0);
                lastSuccessfulHealthPollInstant = now;
                snapshot = healthResult.response();
                if (snapshot.getModules() == null) {
                    snapshot.setModules(createDefaultModules("HEALTHY"));
                }
                snapshot.setStatusReasonCode(healthResult.statusReasonCode());
                snapshot.setStatusMessage(healthResult.statusMessage());
                snapshot.setError(null);

                // Log recovery if prior state was unhealthy
                if (previousFailures > 0 || (!"HEALTHY".equalsIgnoreCase(lastReportedStatus) && !"INIT".equalsIgnoreCase(lastReportedStatus))) {
                    log.info("AI Engine health recovered to {} after {} consecutive failure(s). Message: {}",
                            snapshot.getOverallStatus(), previousFailures, healthResult.statusMessage());
                    lastReportedStatus = snapshot.getOverallStatus();
                    lastReportedReason = healthResult.statusReasonCode();
                    lastWarnLoggedInstant = now;
                }
            } else {
                int failures = consecutiveFailures.incrementAndGet();
                String targetStatus = "SERVICE_UNREACHABLE".equals(healthResult.statusReasonCode()) ? "SERVICE_UNREACHABLE" : "SERVICE_ERROR";

                boolean stateChanged = !targetStatus.equalsIgnoreCase(lastReportedStatus) || !healthResult.statusReasonCode().equals(lastReportedReason);
                boolean periodicWarningDue = now.isAfter(lastWarnLoggedInstant.plus(java.time.Duration.ofMinutes(15)));

                if (stateChanged || periodicWarningDue) {
                    log.warn("AI Engine health check failed [{} - {}]: {}. Marking as {} (failure {} of threshold {}).",
                            healthResult.statusReasonCode(), targetStatus, healthResult.statusMessage(), targetStatus, failures, failureThreshold);
                    lastReportedStatus = targetStatus;
                    lastReportedReason = healthResult.statusReasonCode();
                    lastWarnLoggedInstant = now;
                } else {
                    log.debug("AI Engine health poll failed (failure {}/{}): {}", failures, failureThreshold, healthResult.statusMessage());
                }

                if (failures >= failureThreshold || latestSnapshot.get() == null) {
                    snapshot = createFallbackAiHealth(targetStatus, healthResult.statusReasonCode(), healthResult.statusMessage());
                } else {
                    // Retain previous known healthy snapshot while below failure threshold
                    AdminHealthResponse prev = latestSnapshot.get();
                    if (prev != null) {
                        snapshot = AdminHealthResponse.builder()
                                .overallStatus(prev.getOverallStatus())
                                .service(prev.getService())
                                .checkedAt(prev.getCheckedAt())
                                .lastCheckedAt(now.toString())
                                .isStale(true)
                                .backendStatus("UP")
                                .databaseStatus(dbStatus)
                                .activeUsersCount(activeUsers)
                                .totalUsersCount(totalUsers)
                                .modules(prev.getModules())
                                .providers(prev.getProviders())
                                .environment(prev.getEnvironment())
                                .statusReasonCode(healthResult.statusReasonCode())
                                .statusMessage(healthResult.statusMessage() + " (Failure " + failures + "/" + failureThreshold + ")")
                                .error(healthResult.statusMessage())
                                .build();
                    } else {
                        snapshot = createFallbackAiHealth(targetStatus, healthResult.statusReasonCode(), healthResult.statusMessage());
                    }
                }
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
            snapshot.setLastAttempt(now.toString());
            snapshot.setLastSuccessfulPoll(lastSuccessfulHealthPollInstant != null ? lastSuccessfulHealthPollInstant.toString() : null);
            snapshot.setConsecutiveFailures(consecutiveFailures.get());
            snapshot.setIsStale(snapshot.getIsStale() != null ? snapshot.getIsStale() : false);

            latestSnapshot.set(snapshot);
            lastCheckedInstant = now;
            log.debug("Admin health poll completed at {} (DB: {}, AI Engine: {}, Failures: {})",
                    now, dbStatus, snapshot.getOverallStatus(), consecutiveFailures.get());
        } catch (Exception ex) {
            log.error("Unexpected error during scheduled health poll: {}", ex.getMessage(), ex);
        }
    }

    public Instant getLastSuccessfulHealthPollInstant() {
        return lastSuccessfulHealthPollInstant;
    }

    public Instant getLastAttemptInstant() {
        return lastAttemptInstant;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
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
            snapshot = createFallbackAiHealth("SERVICE_UNREACHABLE", "SERVICE_UNREACHABLE", "AI Engine initial health check pending");
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
                .isStale(stale || Boolean.TRUE.equals(snapshot.getIsStale()))
                .backendStatus(snapshot.getBackendStatus())
                .databaseStatus(snapshot.getDatabaseStatus())
                .activeUsersCount(snapshot.getActiveUsersCount())
                .totalUsersCount(snapshot.getTotalUsersCount())
                .modules(snapshot.getModules())
                .providers(snapshot.getProviders())
                .environment(snapshot.getEnvironment())
                .error(snapshot.getError())
                .statusReasonCode(snapshot.getStatusReasonCode())
                .statusMessage(snapshot.getStatusMessage())
                .lastSuccessfulPoll(snapshot.getLastSuccessfulPoll())
                .lastAttempt(snapshot.getLastAttempt())
                .consecutiveFailures(snapshot.getConsecutiveFailures())
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
        return createFallbackAiHealth("OFFLINE", "SERVICE_UNREACHABLE", "AI Engine unreachable");
    }

    private AdminHealthResponse createFallbackAiHealth(String overallStatus, String reasonCode, String message) {
        String modStatus = "SERVICE_UNREACHABLE".equals(overallStatus) || "OFFLINE".equals(overallStatus) ? "OFFLINE" : "DEGRADED";
        return AdminHealthResponse.builder()
                .service("HireAI AI Engine")
                .overallStatus(overallStatus != null ? overallStatus : "OFFLINE")
                .statusReasonCode(reasonCode != null ? reasonCode : "SERVICE_UNREACHABLE")
                .statusMessage(message != null ? message : "AI Engine unreachable")
                .error(message != null ? message : "AI Engine unreachable")
                .checkedAt(Instant.now().toString())
                .lastCheckedAt(Instant.now().toString())
                .lastAttempt(lastAttemptInstant != null ? lastAttemptInstant.toString() : Instant.now().toString())
                .lastSuccessfulPoll(lastSuccessfulHealthPollInstant != null ? lastSuccessfulHealthPollInstant.toString() : null)
                .consecutiveFailures(consecutiveFailures.get())
                .isStale(false)
                .backendStatus("UP")
                .databaseStatus("UP")
                .activeUsersCount(0L)
                .totalUsersCount(0L)
                .modules(createDefaultModules(modStatus))
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
