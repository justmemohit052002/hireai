package com.vionsys.hireai.security.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages per-account authentication failure tracking and dynamic exponential backoff.
 * Replaces hardcoded lockouts with an escalating delay policy that protects against
 * brute force while enabling legitimate users to retry without permanent lockouts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountBackoffManager {

    private final RateLimitProperties properties;
    private final Map<String, AccountState> accountStates = new ConcurrentHashMap<>();

    @Getter
    @AllArgsConstructor
    public static class BackoffResult {
        private final boolean blocked;
        private final long remainingCooldownSeconds;
        private final int consecutiveFailures;

        public static BackoffResult allowed(int consecutiveFailures) {
            return new BackoffResult(false, 0L, consecutiveFailures);
        }

        public static BackoffResult blocked(long remainingCooldownSeconds, int consecutiveFailures) {
            return new BackoffResult(true, Math.max(1L, remainingCooldownSeconds), consecutiveFailures);
        }
    }

    private static class AccountState {
        private int consecutiveFailures;
        private long lastFailureEpochMs;
        private long backoffUntilEpochMs;

        synchronized void recordSuccess() {
            this.consecutiveFailures = 0;
            this.backoffUntilEpochMs = 0L;
        }
    }

    /**
     * Checks if the given account is currently subject to an active exponential backoff cooldown.
     *
     * @param rawAccount user identifier (e.g. email)
     * @return BackoffResult indicating whether the account is blocked and remaining cooldown seconds
     */
    public BackoffResult checkBackoff(String rawAccount) {
        if (rawAccount == null || rawAccount.isBlank()) {
            return BackoffResult.allowed(0);
        }

        String account = normalize(rawAccount);
        AccountState state = accountStates.get(account);
        if (state == null) {
            return BackoffResult.allowed(0);
        }

        long now = System.currentTimeMillis();
        synchronized (state) {
            if (state.backoffUntilEpochMs > now) {
                long remainingMs = state.backoffUntilEpochMs - now;
                long remainingSec = Math.max(1L, (long) Math.ceil(remainingMs / 1000.0));
                return BackoffResult.blocked(remainingSec, state.consecutiveFailures);
            }
            return BackoffResult.allowed(state.consecutiveFailures);
        }
    }

    /**
     * Records an authentication failure for the given account, escalating the exponential backoff if applicable.
     *
     * @param rawAccount user identifier (e.g. email)
     * @return backoff duration in seconds applied (0 if still within base attempts)
     */
    public long recordFailure(String rawAccount) {
        if (rawAccount == null || rawAccount.isBlank()) {
            return 0L;
        }

        String account = normalize(rawAccount);
        AccountState state = accountStates.computeIfAbsent(account, k -> new AccountState());

        long now = System.currentTimeMillis();
        RateLimitProperties.AccountProperties config = properties.getAuth().getAccount();
        long windowMs = config.getWindowSeconds() * 1000L;

        synchronized (state) {
            // If the last failure was outside the sliding failure window, reset counter
            if (state.lastFailureEpochMs > 0 && (now - state.lastFailureEpochMs) > windowMs) {
                state.consecutiveFailures = 0;
            }

            state.consecutiveFailures++;
            state.lastFailureEpochMs = now;

            if (state.consecutiveFailures > config.getBaseAttempts()) {
                int escalationLevel = state.consecutiveFailures - config.getBaseAttempts() - 1;
                double calculatedSeconds = config.getInitialBackoffSeconds() * Math.pow(config.getMultiplier(), escalationLevel);
                long backoffSeconds = Math.min(config.getMaxBackoffSeconds(), Math.round(calculatedSeconds));

                state.backoffUntilEpochMs = now + (backoffSeconds * 1000L);

                log.warn("Exponential backoff activated for account '{}': failure count={}, backoff={}s",
                        account, state.consecutiveFailures, backoffSeconds);

                return backoffSeconds;
            }

            return 0L;
        }
    }

    /**
     * Resets failure count and backoff state on successful authentication.
     *
     * @param rawAccount user identifier (e.g. email)
     */
    public void recordSuccess(String rawAccount) {
        if (rawAccount == null || rawAccount.isBlank()) {
            return;
        }
        String account = normalize(rawAccount);
        AccountState state = accountStates.get(account);
        if (state != null) {
            state.recordSuccess();
            accountStates.remove(account);
            log.debug("Reset backoff and failure count for account '{}'", account);
        }
    }

    /**
     * Calculates the exponential backoff duration in seconds for a specific failure count based on configuration.
     */
    public long calculateBackoffSeconds(int consecutiveFailures) {
        RateLimitProperties.AccountProperties config = properties.getAuth().getAccount();
        if (consecutiveFailures <= config.getBaseAttempts()) {
            return 0L;
        }
        int escalationLevel = consecutiveFailures - config.getBaseAttempts() - 1;
        double calculated = config.getInitialBackoffSeconds() * Math.pow(config.getMultiplier(), escalationLevel);
        return Math.min(config.getMaxBackoffSeconds(), Math.round(calculated));
    }

    private String normalize(String raw) {
        return raw.trim().toLowerCase();
    }

    /**
     * Periodic cleanup of expired account backoff entries.
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredStates() {
        long cutoff = System.currentTimeMillis() - 1800000L; // 30 minutes
        accountStates.entrySet().removeIf(entry -> {
            AccountState s = entry.getValue();
            synchronized (s) {
                return s.backoffUntilEpochMs < System.currentTimeMillis() && s.lastFailureEpochMs < cutoff;
            }
        });
    }
}
