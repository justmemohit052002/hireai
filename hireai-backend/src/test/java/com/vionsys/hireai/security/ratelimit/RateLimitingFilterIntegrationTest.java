package com.vionsys.hireai.security.ratelimit;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

class RateLimitingFilterIntegrationTest {

    @RestController
    static class DummyAuthController {
        @PostMapping("/auth/login")
        public ResponseEntity<Map<String, Object>> login(@RequestBody(required = false) Map<String, Object> body) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Invalid email or password"
            ));
        }
    }

    private RateLimitProperties properties;
    private RateLimiter rateLimiter;
    private AccountBackoffManager accountBackoffManager;
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setEnabled(true);

        // Strict test quotas
        properties.getAuth().getIp().setCapacity(3);
        properties.getAuth().getIp().setWindowSeconds(60);

        properties.getAuth().getAccount().setBaseAttempts(2);
        properties.getAuth().getAccount().setInitialBackoffSeconds(15);
        properties.getAuth().getAccount().setMultiplier(2.0);
        properties.getAuth().getAccount().setWindowSeconds(300);

        rateLimiter = new RateLimiter();
        accountBackoffManager = new AccountBackoffManager(properties);
        objectMapper = new ObjectMapper().findAndRegisterModules();

        RateLimitingFilter filter = new RateLimitingFilter(
                properties,
                rateLimiter,
                accountBackoffManager,
                objectMapper
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(new DummyAuthController())
                .addFilters(filter)
                .build();
    }

    @Test
    @DisplayName("Repeated bad logins exceeding IP capacity return HTTP 429 with Retry-After header")
    void repeatedBadLoginsExceedingIpCapacityReturns429WithRetryAfter() throws Exception {
        String clientIp = "192.168.1.50";
        String loginPayload = "{\"email\":\"tester@hireai.test\",\"password\":\"wrong-pass\"}";

        // Requests 1 to 3 should pass through filter and return 401 from controller
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginPayload)
                            .with(request -> {
                                request.setRemoteAddr(clientIp);
                                return request;
                            }))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("X-RateLimit-Limit", "3"))
                    .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(3 - i)));
        }

        // Request 4 exceeds IP capacity (3) and must be intercepted by RateLimitingFilter with HTTP 429
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload)
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Too many authentication attempts")));
    }

    @Test
    @DisplayName("Rate-limiting one client does not block another client")
    void oneClientBeingLimitedDoesNotBlockAnotherClient() throws Exception {
        String clientIpA = "192.168.1.10";
        String clientIpB = "192.168.1.20";
        String payload = "{\"email\":\"candidate@hireai.test\",\"password\":\"bad\"}";

        // Exhaust quota for client A
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload)
                            .with(req -> { req.setRemoteAddr(clientIpA); return req; }))
                    .andExpect(status().isUnauthorized());
        }

        // Client A 4th attempt is blocked with 429
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(req -> { req.setRemoteAddr(clientIpA); return req; }))
                .andExpect(status().isTooManyRequests());

        // Client B is completely unaffected and receives 401 (allowed through filter)
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(req -> { req.setRemoteAddr(clientIpB); return req; }))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "2"));
    }

    @Test
    @DisplayName("Target account with exponential backoff is blocked with 429 and Retry-After header")
    void accountExponentialBackoffBlocksTargetAccountWith429() throws Exception {
        String accountEmail = "locked.user@hireai.test";
        String payload = "{\"email\":\"" + accountEmail + "\",\"password\":\"wrong\"}";

        // Simulate 3 failures in AccountBackoffManager (base attempts = 2)
        accountBackoffManager.recordFailure(accountEmail, "10.0.0.1");
        accountBackoffManager.recordFailure(accountEmail, "10.0.0.1");
        long backoff = accountBackoffManager.recordFailure(accountEmail, "10.0.0.1");
        org.junit.jupiter.api.Assertions.assertTrue(backoff > 0, "Backoff must be triggered after exceeding base attempts");

        // Next request for this account is blocked by RateLimitingFilter with HTTP 429
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(req -> { req.setRemoteAddr("10.0.0.1"); return req; }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Exponential backoff active")));

        // Request for a different account from the same IP is still allowed
        String differentPayload = "{\"email\":\"other.user@hireai.test\",\"password\":\"wrong\"}";
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(differentPayload)
                        .with(req -> { req.setRemoteAddr("10.0.0.1"); return req; }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rate limit resets after quota reset")
    void rateLimitResetsAfterQuotaReset() throws Exception {
        String clientIp = "192.168.1.75";
        String payload = "{\"email\":\"reset.test@hireai.test\",\"password\":\"wrong\"}";

        // Exhaust capacity
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
                    .with(req -> { req.setRemoteAddr(clientIp); return req; }));
        }

        // Blocked
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(req -> { req.setRemoteAddr(clientIp); return req; }))
                .andExpect(status().isTooManyRequests());

        // Reset rate limiter for this IP
        rateLimiter.reset("auth:ip:" + clientIp);

        // Allowed again
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(req -> { req.setRemoteAddr(clientIp); return req; }))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "2"));
    }

    @Test
    @DisplayName("Spoofed forwarded headers from untrusted peer cannot change the rate limiting key")
    void spoofedForwardedHeadersFromUntrustedPeerCannotChangeKey() {
        RateLimitingFilter filter = new RateLimitingFilter(properties, rateLimiter, accountBackoffManager, objectMapper);
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.195");
        request.addHeader("X-Forwarded-For", "8.8.8.8, 1.1.1.1");
        request.addHeader("CF-Connecting-IP", "9.9.9.9");
        request.addHeader("X-Real-IP", "10.0.0.1");

        // properties.getTrustedProxies() is empty by default -> direct peer remoteAddr is strictly used
        String resolvedIp = filter.resolveClientIp(request);
        org.junit.jupiter.api.Assertions.assertEquals("203.0.113.195", resolvedIp);
    }

    @Test
    @DisplayName("Headers from trusted proxy are honored with right-most untrusted entry selected")
    void headersFromTrustedProxyHonoredWithRightMostUntrustedEntry() {
        properties.getTrustedProxies().add("127.0.0.1");
        properties.getTrustedProxies().add("10.0.0.1");

        RateLimitingFilter filter = new RateLimitingFilter(properties, rateLimiter, accountBackoffManager, objectMapper);
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        // Far left is spoofed (8.8.8.8), middle is real client (198.51.100.25), right is intermediate proxy (10.0.0.1)
        request.addHeader("X-Forwarded-For", "8.8.8.8, 198.51.100.25, 10.0.0.1");

        String resolvedIp = filter.resolveClientIp(request);
        org.junit.jupiter.api.Assertions.assertEquals("198.51.100.25", resolvedIp);
    }

    @Test
    @DisplayName("IPv6 and malformed forwarded headers are handled safely")
    void ipv6AndMalformedForwardedHeadersHandledSafely() {
        properties.getTrustedProxies().add("127.0.0.1");
        RateLimitingFilter filter = new RateLimitingFilter(properties, rateLimiter, accountBackoffManager, objectMapper);

        // Valid IPv6 with port
        org.springframework.mock.web.MockHttpServletRequest reqIpv6 = new org.springframework.mock.web.MockHttpServletRequest();
        reqIpv6.setRemoteAddr("127.0.0.1");
        reqIpv6.addHeader("X-Forwarded-For", "[2001:db8::1]:8080");
        org.junit.jupiter.api.Assertions.assertEquals("2001:db8::1", filter.resolveClientIp(reqIpv6));

        // Malformed header entries safely fall back to direct peer
        org.springframework.mock.web.MockHttpServletRequest reqMalformed = new org.springframework.mock.web.MockHttpServletRequest();
        reqMalformed.setRemoteAddr("127.0.0.1");
        reqMalformed.addHeader("X-Forwarded-For", "../../malformed, unknown");
        org.junit.jupiter.api.Assertions.assertEquals("127.0.0.1", filter.resolveClientIp(reqMalformed));
    }

    @Test
    @DisplayName("Attacker cannot lock out legitimate user logging in from another IP")
    void attackerCannotLockOutVictimFromAnotherIp() throws Exception {
        String victimEmail = "target.user@hireai.test";
        String attackerIp = "198.51.100.77";
        String legitimateUserIp = "203.0.113.88";
        String payload = "{\"email\":\"" + victimEmail + "\",\"password\":\"wrong\"}";

        // Attacker fails 3 times from attackerIp (base attempts = 2)
        accountBackoffManager.recordFailure(victimEmail, attackerIp);
        accountBackoffManager.recordFailure(victimEmail, attackerIp);
        accountBackoffManager.recordFailure(victimEmail, attackerIp);

        // Attacker IP is blocked by RateLimitingFilter with HTTP 429
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(req -> { req.setRemoteAddr(attackerIp); return req; }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status", is(429)));

        // Legitimate user from another IP is NOT blocked and reaches authentication (HTTP 401)
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(req -> { req.setRemoteAddr(legitimateUserIp); return req; }))
                .andExpect(status().isUnauthorized());
    }
}
