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
public class AccountBackoffManager {

    private final RateLimitProperties properties;
    private final Map<String, AccountState> accountIpStates = new ConcurrentHashMap<>();
    private final Map<String, AccountState> globalAccountStates = new ConcurrentHashMap<>();

    public AccountBackoffManager(RateLimitProperties properties) {
        this.properties = properties;
    }

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
     * Checks if the given account and client IP are currently subject to an active exponential backoff cooldown.
     *
     * @param rawAccount user identifier (e.g. email)
     * @param clientIp   client IP address
     * @return BackoffResult indicating whether the request is blocked and remaining cooldown seconds
     */
    public BackoffResult checkBackoff(String rawAccount, String clientIp) {
        if (rawAccount == null || rawAccount.isBlank()) {
            return BackoffResult.allowed(0);
        }

        String account = normalize(rawAccount);
        long now = System.currentTimeMillis();

        // 1. Check global per-account safety net (protects against distributed attacks across many IPs)
        AccountState globalState = globalAccountStates.get(account);
        if (globalState != null) {
            synchronized (globalState) {
                if (globalState.backoffUntilEpochMs > now) {
                    long remainingMs = globalState.backoffUntilEpochMs - now;
                    long remainingSec = Math.max(1L, (long) Math.ceil(remainingMs / 1000.0));
                    return BackoffResult.blocked(remainingSec, globalState.consecutiveFailures);
                }
            }
        }

        // 2. Check per-(account + client IP) backoff (prevents single IP from locking out legitimate user)
        String effectiveIp = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "default";
        String ipKey = account + ":" + effectiveIp;
        AccountState ipState = accountIpStates.get(ipKey);
        if (ipState != null) {
            synchronized (ipState) {
                if (ipState.backoffUntilEpochMs > now) {
                    long remainingMs = ipState.backoffUntilEpochMs - now;
                    long remainingSec = Math.max(1L, (long) Math.ceil(remainingMs / 1000.0));
                    return BackoffResult.blocked(remainingSec, ipState.consecutiveFailures);
                }
            }
        }

        return BackoffResult.allowed(0);
    }

    public BackoffResult checkBackoff(String rawAccount) {
        return checkBackoff(rawAccount, null);
    }

    /**
     * Records an authentication failure for the given account and client IP.
     * Escalates exponential backoff per IP, and activates global safety net if threshold is exceeded.
     *
     * @param rawAccount user identifier (e.g. email)
     * @param clientIp   client IP address
     * @return applied backoff duration in seconds (0 if within limits)
     */
    public long recordFailure(String rawAccount, String clientIp) {
        if (rawAccount == null || rawAccount.isBlank()) {
            return 0L;
        }

        String account = normalize(rawAccount);
        long now = System.currentTimeMillis();
        RateLimitProperties.AccountProperties config = properties.getAuth().getAccount();
        long windowMs = config.getWindowSeconds() * 1000L;

        // 1. Record in global safety net
        long globalBackoff = 0L;
        AccountState globalState = globalAccountStates.computeIfAbsent(account, k -> new AccountState());
        synchronized (globalState) {
            if (globalState.lastFailureEpochMs > 0 && (now - globalState.lastFailureEpochMs) > windowMs) {
                globalState.consecutiveFailures = 0;
            }
            globalState.consecutiveFailures++;
            globalState.lastFailureEpochMs = now;

            if (globalState.consecutiveFailures > config.getGlobalMaxAttempts()) {
                int escalationLevel = globalState.consecutiveFailures - config.getGlobalMaxAttempts() - 1;
                double calculatedSeconds = config.getInitialBackoffSeconds() * Math.pow(config.getMultiplier(), escalationLevel);
                globalBackoff = Math.min(config.getMaxBackoffSeconds(), Math.round(calculatedSeconds));
                globalState.backoffUntilEpochMs = now + (globalBackoff * 1000L);

                log.warn("Global safety net exponential backoff activated for account '{}': failures={}, backoff={}s",
                        account, globalState.consecutiveFailures, globalBackoff);
            }
        }

        // 2. Record in per (account + client IP)
        long ipBackoff = 0L;
        String effectiveIp = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "default";
        String ipKey = account + ":" + effectiveIp;
        AccountState ipState = accountIpStates.computeIfAbsent(ipKey, k -> new AccountState());
        synchronized (ipState) {
            if (ipState.lastFailureEpochMs > 0 && (now - ipState.lastFailureEpochMs) > windowMs) {
                ipState.consecutiveFailures = 0;
            }
            ipState.consecutiveFailures++;
            ipState.lastFailureEpochMs = now;

            if (ipState.consecutiveFailures > config.getBaseAttempts()) {
                int escalationLevel = ipState.consecutiveFailures - config.getBaseAttempts() - 1;
                double calculatedSeconds = config.getInitialBackoffSeconds() * Math.pow(config.getMultiplier(), escalationLevel);
                ipBackoff = Math.min(config.getMaxBackoffSeconds(), Math.round(calculatedSeconds));
                ipState.backoffUntilEpochMs = now + (ipBackoff * 1000L);

                log.warn("Exponential backoff activated for account '{}' from IP {}: failures={}, backoff={}s",
                        account, effectiveIp, ipState.consecutiveFailures, ipBackoff);
            }
        }

        return Math.max(globalBackoff, ipBackoff);
    }

    public long recordFailure(String rawAccount) {
        return recordFailure(rawAccount, null);
    }

    /**
     * Resets failure count and backoff state on successful authentication.
     *
     * @param rawAccount user identifier (e.g. email)
     * @param clientIp   client IP address
     */
    public void recordSuccess(String rawAccount, String clientIp) {
        if (rawAccount == null || rawAccount.isBlank()) {
            return;
        }
        String account = normalize(rawAccount);
        globalAccountStates.remove(account);
        if (clientIp != null && !clientIp.isBlank()) {
            accountIpStates.remove(account + ":" + clientIp.trim());
            accountIpStates.remove(account + ":default");
        } else {
            accountIpStates.keySet().removeIf(k -> k.startsWith(account + ":"));
        }
        log.debug("Reset backoff and failure count for account '{}'", account);
    }

    public void recordSuccess(String rawAccount) {
        recordSuccess(rawAccount, null);
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
        globalAccountStates.entrySet().removeIf(entry -> {
            AccountState s = entry.getValue();
            synchronized (s) {
                return s.backoffUntilEpochMs < System.currentTimeMillis() && s.lastFailureEpochMs < cutoff;
            }
        });
        accountIpStates.entrySet().removeIf(entry -> {
            AccountState s = entry.getValue();
            synchronized (s) {
                return s.backoffUntilEpochMs < System.currentTimeMillis() && s.lastFailureEpochMs < cutoff;
            }
        });
    }
}
