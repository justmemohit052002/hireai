package com.vionsys.hireai.email;

import java.time.Instant;

/**
 * Outbound email transport port contract.
 */
public interface EmailTransport {

    /**
     * Dispatch an outbound email message synchronously through this transport.
     *
     * @param message Prepared message with headers and dual HTML/plain-text bodies
     */
    void dispatch(EmailMessage message);

    /**
     * Check if the transport is properly configured and ready for outbound delivery.
     */
    boolean isReady();

    /**
     * Returns the transport delivery mode (SMTP or LOG).
     */
    EmailMode getMode();

    /**
     * Perform a connectivity/health check against the outbound transport provider.
     */
    void checkConnection();

    /**
     * Timestamp of the most recent successful email dispatch.
     */
    Instant getLastSuccessfulSend();

    /**
     * Count of recent consecutive delivery failures.
     */
    long getRecentFailureCount();

    /**
     * Lifetime counter of successfully dispatched messages.
     */
    long getTotalSentCount();

    /**
     * Lifetime counter of failed message deliveries.
     */
    long getTotalFailedCount();
}
