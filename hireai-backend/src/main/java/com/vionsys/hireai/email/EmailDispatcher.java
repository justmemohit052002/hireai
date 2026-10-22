package com.vionsys.hireai.email;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dispatcher bean that handles thread boundary traversal via Spring's async proxy.
 * Ensures all @Async calls are invoked across the bean boundary on the dedicated emailTaskExecutor pool.
 */
@Slf4j
@Component
public class EmailDispatcher {

    private final SmtpEmailTransport smtpEmailTransport;
    private final LogEmailTransport logEmailTransport;
    private final EmailProperties emailProperties;

    public EmailDispatcher(
            SmtpEmailTransport smtpEmailTransport,
            LogEmailTransport logEmailTransport,
            EmailProperties emailProperties) {
        this.smtpEmailTransport = smtpEmailTransport;
        this.logEmailTransport = logEmailTransport;
        this.emailProperties = emailProperties;
    }

    /**
     * Dispatch an email asynchronously on the dedicated hireai-mail executor thread pool.
     * Non-critical delivery failures are logged and do not impact user transactions.
     */
    @Async("emailTaskExecutor")
    public void dispatchAsync(EmailMessage message) {
        if (!emailProperties.isEnabled() || message == null || message.getTo() == null) {
            return;
        }

        try {
            resolveTransport().dispatch(message);
        } catch (Exception ex) {
            log.warn("[{}] Non-critical async email dispatch failed for {}: {}",
                    message.getCorrelationId(), EmailServiceImpl.maskEmail(message.getTo()), ex.getMessage());
        }
    }

    /**
     * Dispatch an email synchronously on the calling thread.
     * Used for critical authentication flows (e.g. registration OTP) and admin diagnostic test sends.
     */
    public void dispatchSync(EmailMessage message) {
        if (!emailProperties.isEnabled() || message == null || message.getTo() == null) {
            return;
        }

        resolveTransport().dispatch(message);
    }

    /**
     * Resolve the active transport based on typed application configuration.
     */
    public EmailTransport resolveTransport() {
        if (emailProperties.getMode() == EmailMode.LOG) {
            return logEmailTransport;
        }
        return smtpEmailTransport;
    }
}
