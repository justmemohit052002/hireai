package com.vionsys.hireai.security.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RateLimitResult {

    private final boolean allowed;
    private final int limit;
    private final int remaining;
    private final long retryAfterSeconds;
    private final long resetEpochSeconds;
    private final String reason;

    public static RateLimitResult allowed(int limit, int remaining, long resetEpochSeconds) {
        return RateLimitResult.builder()
                .allowed(true)
                .limit(limit)
                .remaining(Math.max(0, remaining))
                .retryAfterSeconds(0)
                .resetEpochSeconds(resetEpochSeconds)
                .build();
    }

    public static RateLimitResult rejected(int limit, long retryAfterSeconds, long resetEpochSeconds, String reason) {
        return RateLimitResult.builder()
                .allowed(false)
                .limit(limit)
                .remaining(0)
                .retryAfterSeconds(Math.max(1, retryAfterSeconds))
                .resetEpochSeconds(resetEpochSeconds)
                .reason(reason)
                .build();
    }
}
