package com.vionsys.hireai.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hireai.rate-limit")
public class RateLimitProperties {

    /**
     * Master switch to enable or disable rate limiting globally.
     */
    private boolean enabled = true;

    /**
     * Rate limiting policies for authentication routes (login, register, forgot-password, reset-password).
     */
    private AuthProperties auth = new AuthProperties();

    /**
     * Rate limiting policies for public endpoints.
     */
    private PublicProperties publicEndpoints = new PublicProperties();

    /**
     * Rate limiting policies for authenticated user requests.
     */
    private AuthenticatedProperties authenticated = new AuthenticatedProperties();

    @Getter
    @Setter
    public static class AuthProperties {
        /**
         * Per-IP rate limiting configuration for auth routes.
         */
        private IpProperties ip = new IpProperties();

        /**
         * Per-Account rate limiting with exponential backoff configuration.
         */
        private AccountProperties account = new AccountProperties();
    }

    @Getter
    @Setter
    public static class IpProperties {
        /**
         * Maximum allowed requests per IP within the window.
         */
        private int capacity = 15;

        /**
         * Sliding window duration in seconds.
         */
        private long windowSeconds = 60;
    }

    @Getter
    @Setter
    public static class AccountProperties {
        /**
         * Number of failed attempts allowed before exponential backoff is triggered.
         */
        private int baseAttempts = 5;

        /**
         * Sliding window duration in seconds for tracking attempts.
         */
        private long windowSeconds = 300;

        /**
         * Initial backoff delay in seconds upon exceeding base attempts.
         */
        private long initialBackoffSeconds = 15;

        /**
         * Multiplier for each subsequent failed attempt (e.g., 2.0 doubles each time).
         */
        private double multiplier = 2.0;

        /**
         * Maximum backoff delay in seconds (ceiling). Default: 900 seconds (15 minutes).
         */
        private long maxBackoffSeconds = 900;
    }

    @Getter
    @Setter
    public static class PublicProperties {
        /**
         * Maximum allowed requests for public endpoints per IP within the window.
         */
        private int capacity = 60;

        /**
         * Sliding window duration in seconds.
         */
        private long windowSeconds = 60;
    }

    @Getter
    @Setter
    public static class AuthenticatedProperties {
        /**
         * Maximum allowed requests for authenticated user actions per user within the window.
         */
        private int capacity = 180;

        /**
         * Sliding window duration in seconds.
         */
        private long windowSeconds = 60;
    }
}
