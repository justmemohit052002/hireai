package com.vionsys.hireai.security.ratelimit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountBackoffManagerTest {

    private RateLimitProperties properties;
    private AccountBackoffManager backoffManager;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        // Configure test settings: 3 base attempts, initial backoff 10s, multiplier 2.0, max backoff 120s
        properties.getAuth().getAccount().setBaseAttempts(3);
        properties.getAuth().getAccount().setInitialBackoffSeconds(10);
        properties.getAuth().getAccount().setMultiplier(2.0);
        properties.getAuth().getAccount().setMaxBackoffSeconds(120);
        properties.getAuth().getAccount().setWindowSeconds(300);

        backoffManager = new AccountBackoffManager(properties);
    }

    @Test
    @DisplayName("Failures within base attempts should not trigger backoff")
    void failuresWithinBaseAttemptsShouldNotTriggerBackoff() {
        String email = "john.doe@example.com";

        // Attempt 1, 2, 3 should return 0 backoff and not be blocked
        assertEquals(0, backoffManager.recordFailure(email));
        assertFalse(backoffManager.checkBackoff(email).isBlocked());

        assertEquals(0, backoffManager.recordFailure(email));
        assertFalse(backoffManager.checkBackoff(email).isBlocked());

        assertEquals(0, backoffManager.recordFailure(email));
        assertFalse(backoffManager.checkBackoff(email).isBlocked());
    }

    @Test
    @DisplayName("Failures exceeding base attempts should escalate with exponential backoff")
    void failuresExceedingBaseAttemptsShouldEscalateExponentially() {
        String email = "attacker@example.com";

        // First 3 failures: within base attempts
        backoffManager.recordFailure(email);
        backoffManager.recordFailure(email);
        backoffManager.recordFailure(email);

        // 4th failure: 1st violation -> initialBackoff = 10s
        long b4 = backoffManager.recordFailure(email);
        assertEquals(10, b4);
        AccountBackoffManager.BackoffResult check4 = backoffManager.checkBackoff(email);
        assertTrue(check4.isBlocked());
        assertTrue(check4.getRemainingCooldownSeconds() > 0 && check4.getRemainingCooldownSeconds() <= 10);

        // 5th failure: 2nd violation -> 10 * 2.0^1 = 20s
        long b5 = backoffManager.recordFailure(email);
        assertEquals(20, b5);

        // 6th failure: 3rd violation -> 10 * 2.0^2 = 40s
        long b6 = backoffManager.recordFailure(email);
        assertEquals(40, b6);

        // 7th failure: 4th violation -> 10 * 2.0^3 = 80s
        long b7 = backoffManager.recordFailure(email);
        assertEquals(80, b7);

        // 8th failure: 5th violation -> 10 * 2.0^4 = 160s, capped at maxBackoff (120s)
        long b8 = backoffManager.recordFailure(email);
        assertEquals(120, b8);
    }

    @Test
    @DisplayName("Successful login should immediately reset failure count and clear backoff")
    void recordSuccessShouldClearBackoffImmediately() {
        String email = "sarah.connor@example.com";

        // Exceed base attempts
        for (int i = 0; i < 5; i++) {
            backoffManager.recordFailure(email);
        }
        assertTrue(backoffManager.checkBackoff(email).isBlocked());

        // On successful authentication
        backoffManager.recordSuccess(email);
        assertFalse(backoffManager.checkBackoff(email).isBlocked());
    }

    @Test
    @DisplayName("Should handle case-insensitive email normalization")
    void shouldNormalizeEmailAddresses() {
        String mixedEmail = "User.Test@Example.COM";
        String lowerEmail = "user.test@example.com";

        backoffManager.recordFailure(mixedEmail);
        backoffManager.recordFailure(lowerEmail);
        backoffManager.recordFailure(mixedEmail);

        // 4th failure triggers backoff regardless of case
        long backoff = backoffManager.recordFailure(lowerEmail);
        assertEquals(10, backoff);

        assertTrue(backoffManager.checkBackoff(mixedEmail).isBlocked());
        assertTrue(backoffManager.checkBackoff(lowerEmail).isBlocked());
    }
}
