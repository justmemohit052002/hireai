package com.vionsys.hireai.security.ratelimit;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-safe in-memory Sliding Window rate limiter.
 * Tracks discrete request timestamps per key to prevent burst exploitation
 * and ensure fair request distribution across the configured window.
 */
@Slf4j
@Component
public class RateLimiter {

    private final Map<String, Deque<Long>> requestWindows = new ConcurrentHashMap<>();

    /**
     * Attempts to consume one request token for the given key under the specified capacity and window.
     *
     * @param key           unique key (e.g. IP, user ID)
     * @param capacity      max requests allowed in window
     * @param windowSeconds sliding window duration in seconds
     * @return RateLimitResult indicating whether request is allowed and metadata (remaining, retry-after)
     */
    public RateLimitResult tryConsume(String key, int capacity, long windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;
        long windowStart = now - windowMs;

        Deque<Long> timestamps = requestWindows.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Evict timestamps outside the sliding window
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < capacity) {
                timestamps.addLast(now);
                int remaining = capacity - timestamps.size();
                long resetEpochSeconds = (now + windowMs) / 1000L;
                return RateLimitResult.allowed(capacity, remaining, resetEpochSeconds);
            } else {
                Long oldest = timestamps.peekFirst();
                long oldestTime = oldest != null ? oldest : now;
                long retryAfterMs = (oldestTime + windowMs) - now;
                long retryAfterSeconds = Math.max(1L, (long) Math.ceil(retryAfterMs / 1000.0));
                long resetEpochSeconds = (oldestTime + windowMs) / 1000L;

                return RateLimitResult.rejected(
                        capacity,
                        retryAfterSeconds,
                        resetEpochSeconds,
                        "Rate limit exceeded. Maximum " + capacity + " requests allowed per " + windowSeconds + " seconds."
                );
            }
        }
    }

    /**
     * Resets rate limit tracking for a specific key (useful on successful authentication or administrative reset).
     */
    public void reset(String key) {
        requestWindows.remove(key);
    }

    /**
     * Periodic background cleanup every 5 minutes to purge stale rate limit records
     * for clients that haven't made requests in more than 10 minutes.
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupStaleEntries() {
        long staleCutoff = System.currentTimeMillis() - 600000L; // 10 minutes ago
        requestWindows.entrySet().removeIf(entry -> {
            Deque<Long> deque = entry.getValue();
            synchronized (deque) {
                return deque.isEmpty() || deque.peekLast() < staleCutoff;
            }
        });
        log.debug("Purged stale rate limiter entries. Active tracking keys: {}", requestWindows.size());
    }

    /**
     * Returns the current number of active tracked keys.
     */
    public int getActiveKeyCount() {
        return requestWindows.size();
    }
}
