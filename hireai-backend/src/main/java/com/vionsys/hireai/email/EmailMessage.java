package com.vionsys.hireai.email;

import lombok.Builder;
import lombok.Getter;

/**
 * Immutable DTO representing a prepared outbound email message ready for transport dispatch.
 */
@Getter
@Builder
public class EmailMessage {

    /**
     * Recipient email address.
     */
    private final String to;

    /**
     * Email subject line (must not contain CRLF characters).
     */
    private final String subject;

    /**
     * HTML formatted email content.
     */
    private final String htmlContent;

    /**
     * Plain-text alternative fallback content for non-HTML mail clients.
     */
    private final String plainTextContent;

    /**
     * Semantic email classification (e.g. VERIFICATION_OTP, APPLICATION_RECEIVED, LOGIN_ALERT).
     */
    private final String emailType;

    /**
     * Unique correlation identifier for distributed tracing and observability logs.
     */
    private final String correlationId;

    /**
     * Unmasked verification OTP code or security token.
     * Note: This field is only logged when LOG mode is active under dev or test profiles.
     */
    private final String sensitiveToken;
}
