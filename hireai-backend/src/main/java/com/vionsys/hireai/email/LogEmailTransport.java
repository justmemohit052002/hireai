package com.vionsys.hireai.email;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Development and testing email transport that logs dispatches to console without sending network packets.
 */
@Slf4j
@Component("logEmailTransport")
public class LogEmailTransport implements EmailTransport {

    private final EmailProperties emailProperties;
    private final Environment environment;

    private final AtomicLong totalSentCount = new AtomicLong(0);
    private final AtomicLong totalFailedCount = new AtomicLong(0);
    private final AtomicLong recentFailureCount = new AtomicLong(0);
    private volatile Instant lastSuccessfulSend;

    public LogEmailTransport(EmailProperties emailProperties, Environment environment) {
        this.emailProperties = emailProperties;
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        if (emailProperties.getMode() == EmailMode.LOG) {
            log.warn("""
                ======================================================================
                [WARNING] HireAI Email Service is operating in LOG mode.
                All outbound emails are simulated and logged to the application log.
                Real network delivery is DISABLED. Set HIREAI_MAIL_MODE=SMTP for real delivery.
                ======================================================================
                """);
        }
    }

    @Override
    public void dispatch(EmailMessage message) {
        if (!emailProperties.isEnabled()) {
            log.info("[LOG TRANSPORT] Email service disabled via configuration. Skipping dispatch to {}",
                    EmailServiceImpl.maskEmail(message.getTo()));
            return;
        }

        String maskedRecipient = EmailServiceImpl.maskEmail(message.getTo());
        boolean isDevOrTest = environment.acceptsProfiles(Profiles.of("dev", "test"));

        StringBuilder sb = new StringBuilder();
        sb.append("\n[SIMULATED EMAIL DISPATCH] (Mode: LOG - Network Delivery Disabled)\n");
        sb.append("======================================================================\n");
        sb.append(String.format("TYPE:           %s\n", message.getEmailType() != null ? message.getEmailType() : "GENERIC"));
        sb.append(String.format("CORRELATION ID: %s\n", message.getCorrelationId()));
        sb.append(String.format("TO:             %s\n", maskedRecipient));
        sb.append(String.format("FROM:           %s <%s>\n", emailProperties.getSenderName(), emailProperties.getFrom()));
        sb.append(String.format("SUBJECT:        %s\n", message.getSubject()));

        if (message.getSensitiveToken() != null && !message.getSensitiveToken().isBlank()) {
            if (isDevOrTest) {
                sb.append(String.format("SECURITY CODE:  %s (Active Dev/Test Profile)\n", message.getSensitiveToken()));
            } else {
                sb.append("SECURITY CODE:  [PROTECTED - Dev/Test Profile Only]\n");
            }
        }
        sb.append("======================================================================");

        log.info(sb.toString());

        totalSentCount.incrementAndGet();
        recentFailureCount.set(0);
        lastSuccessfulSend = Instant.now();
    }

    @Override
    public boolean isReady() {
        return emailProperties.isEnabled();
    }

    @Override
    public EmailMode getMode() {
        return EmailMode.LOG;
    }

    @Override
    public void checkConnection() {
        log.info("LOG email transport connectivity check: Ready (Console Simulation).");
    }

    @Override
    public Instant getLastSuccessfulSend() {
        return lastSuccessfulSend;
    }

    @Override
    public long getRecentFailureCount() {
        return recentFailureCount.get();
    }

    @Override
    public long getTotalSentCount() {
        return totalSentCount.get();
    }

    @Override
    public long getTotalFailedCount() {
        return totalFailedCount.get();
    }
}
