package com.vionsys.hireai.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "hireai.mail")
public class EmailProperties {

    /**
     * Master switch to enable or disable automated emails across the platform.
     */
    private boolean enabled = true;

    /**
     * Outbound delivery mode: SMTP (real network delivery) or LOG (simulated/console).
     */
    @NotNull
    private EmailMode mode = EmailMode.SMTP;

    /**
     * Default RFC-compliant From address used for outbound messages.
     */
    @NotBlank
    private String from = "notifications@vionsys.com";

    /**
     * Sender display name presented to email recipients.
     */
    @NotBlank
    private String senderName = "HireAI Recruitment Platform";

    /**
     * Frontend application base URL used to construct clickable CTA buttons and dashboard links.
     */
    @NotBlank
    private String frontendBaseUrl = "http://localhost:5173";

    /**
     * Connection timeout in milliseconds for the SMTP transport socket.
     */
    private int connectionTimeoutMs = 10000;

    /**
     * Read timeout in milliseconds for the SMTP transport socket.
     */
    private int readTimeoutMs = 15000;

    /**
     * Write timeout in milliseconds for the SMTP transport socket.
     */
    private int writeTimeoutMs = 15000;

    /**
     * Maximum retry attempts for transient delivery failures (e.g. socket timeout, network blip).
     */
    private int maxRetries = 3;

    /**
     * Initial retry delay in milliseconds between transient delivery attempts.
     */
    private long retryDelayMs = 1000L;

    /**
     * When true, missing or blank SMTP credentials in SMTP mode will cause application startup to fail fast.
     * When false (default), a prominent ERROR banner is logged and runtime exceptions are raised on attempt.
     */
    private boolean failFastOnMissingCredentials = false;
}
