package com.vionsys.hireai.security.ratelimit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
    }

    @Test
    @DisplayName("Should allow requests within capacity and decrement remaining allowance")
    void shouldAllowRequestsWithinCapacity() {
        String key = "test:ip:192.168.1.1";
        int capacity = 3;
        long windowSeconds = 60;

        RateLimitResult r1 = rateLimiter.tryConsume(key, capacity, windowSeconds);
        assertTrue(r1.isAllowed());
        assertEquals(2, r1.getRemaining());

        RateLimitResult r2 = rateLimiter.tryConsume(key, capacity, windowSeconds);
        assertTrue(r2.isAllowed());
        assertEquals(1, r2.getRemaining());

        RateLimitResult r3 = rateLimiter.tryConsume(key, capacity, windowSeconds);
        assertTrue(r3.isAllowed());
        assertEquals(0, r3.getRemaining());
    }

    @Test
    @DisplayName("Should reject requests exceeding capacity and return retry-after")
    void shouldRejectRequestsExceedingCapacity() {
        String key = "test:ip:192.168.1.2";
        int capacity = 2;
        long windowSeconds = 60;

        rateLimiter.tryConsume(key, capacity, windowSeconds);
        rateLimiter.tryConsume(key, capacity, windowSeconds);

        // 3rd attempt should exceed capacity
        RateLimitResult r3 = rateLimiter.tryConsume(key, capacity, windowSeconds);
        assertFalse(r3.isAllowed());
        assertEquals(0, r3.getRemaining());
        assertTrue(r3.getRetryAfterSeconds() > 0 && r3.getRetryAfterSeconds() <= 60);
        assertNotNull(r3.getReason());
    }

    @Test
    @DisplayName("Different keys should maintain isolated rate limit quotas")
    void shouldIsolateRateLimitsForDifferentKeys() {
        String keyA = "test:ip:10.0.0.1";
        String keyB = "test:ip:10.0.0.2";
        int capacity = 1;
        long windowSeconds = 60;

        RateLimitResult rA1 = rateLimiter.tryConsume(keyA, capacity, windowSeconds);
        assertTrue(rA1.isAllowed());

        RateLimitResult rA2 = rateLimiter.tryConsume(keyA, capacity, windowSeconds);
        assertFalse(rA2.isAllowed());

        // Key B should still be allowed
        RateLimitResult rB1 = rateLimiter.tryConsume(keyB, capacity, windowSeconds);
        assertTrue(rB1.isAllowed());
    }

    @Test
    @DisplayName("Reset should immediately restore capacity for key")
    void shouldResetKeyCapacity() {
        String key = "test:ip:10.0.0.99";
        int capacity = 1;
        long windowSeconds = 60;

        rateLimiter.tryConsume(key, capacity, windowSeconds);
        assertFalse(rateLimiter.tryConsume(key, capacity, windowSeconds).isAllowed());

        rateLimiter.reset(key);
        assertTrue(rateLimiter.tryConsume(key, capacity, windowSeconds).isAllowed());
    }
}
