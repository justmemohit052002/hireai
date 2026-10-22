package com.vionsys.hireai.email;

/**
 * Outbound delivery mode for the HireAI email notification service.
 */
public enum EmailMode {
    /**
     * Real delivery via SMTP transport (e.g. Gmail SMTP, SendGrid, Amazon SES, or local Mailpit/GreenMail).
     */
    SMTP,

    /**
     * Clearly labeled console logging transport for local development and testing environments.
     * No network connections are made; messages and masked recipients are logged.
     */
    LOG
}
