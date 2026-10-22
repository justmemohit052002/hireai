package com.vionsys.hireai.email;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.vionsys.hireai.exception.EmailDeliveryException;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Production-grade SMTP transport with exponential backoff retries, permanent failure discrimination,
 * header injection sanitization, dual-body MIME creation, and connection diagnostics.
 */
@Slf4j
@Component("smtpEmailTransport")
public class SmtpEmailTransport implements EmailTransport {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailProperties emailProperties;

    private final AtomicLong totalSentCount = new AtomicLong(0);
    private final AtomicLong totalFailedCount = new AtomicLong(0);
    private final AtomicLong recentFailureCount = new AtomicLong(0);
    private volatile Instant lastSuccessfulSend;

    public SmtpEmailTransport(ObjectProvider<JavaMailSender> mailSenderProvider, EmailProperties emailProperties) {
        this.mailSenderProvider = mailSenderProvider;
        this.emailProperties = emailProperties;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (emailProperties.getMode() != EmailMode.SMTP) {
            return;
        }

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        boolean hasValidCredentials = false;

        if (sender instanceof JavaMailSenderImpl impl) {
            String password = impl.getPassword();
            String host = impl.getHost();
            hasValidCredentials = host != null && !host.isBlank()
                    && password != null && !password.isBlank();
        }

        if (!hasValidCredentials) {
            if (emailProperties.isFailFastOnMissingCredentials()) {
                throw new IllegalStateException(
                        "HireAI mail delivery is configured in SMTP mode, but spring.mail.password is missing or blank."
                );
            } else {
                log.error("""
                    ****************************************************************************************
                    [ERROR] HireAI Email Service: Outbound mode is set to SMTP, but credentials or host
                    are incomplete (spring.mail.password is blank). Real delivery will fail until credentials
                    are supplied. Set SPRING_MAIL_USERNAME and SPRING_MAIL_PASSWORD, or set HIREAI_MAIL_MODE=LOG.
                    ****************************************************************************************
                    """);
            }
        } else {
            log.info("HireAI Email Service initialized in SMTP mode with host: {}",
                    ((JavaMailSenderImpl) sender).getHost());
            performNonFatalConnectivityCheck(sender);
        }
    }

    private void performNonFatalConnectivityCheck(JavaMailSender sender) {
        if (sender instanceof JavaMailSenderImpl impl) {
            try {
                impl.testConnection();
                log.info("SMTP handshake check to {}:{} SUCCESSFUL", impl.getHost(), impl.getPort());
            } catch (Exception ex) {
                log.warn("SMTP initial connectivity check failed for {}:{}: {}. Service will retry on demand.",
                        impl.getHost(), impl.getPort(), ex.getMessage());
            }
        }
    }

    @Override
    public void dispatch(EmailMessage message) {
        if (!emailProperties.isEnabled()) {
            log.info("Email dispatch disabled via configuration. Skipping message to {}",
                    EmailServiceImpl.maskEmail(message.getTo()));
            return;
        }

        String safeTo = sanitizeHeader(message.getTo());
        String safeSubject = sanitizeHeader(message.getSubject());
        String correlationId = message.getCorrelationId() != null ? message.getCorrelationId() : "untracked";
        String emailType = message.getEmailType() != null ? message.getEmailType() : "UNKNOWN";

        if (safeTo == null || !safeTo.contains("@")) {
            log.error("[{}] Permanent delivery failure: invalid recipient address '{}'", correlationId, safeTo);
            totalFailedCount.incrementAndGet();
            recentFailureCount.incrementAndGet();
            throw new EmailDeliveryException("Invalid recipient email address.");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("[{}] JavaMailSender bean unavailable", correlationId);
            totalFailedCount.incrementAndGet();
            recentFailureCount.incrementAndGet();
            throw new EmailDeliveryException("We could not send the verification email. Please try Resend.");
        }

        if (mailSender instanceof JavaMailSenderImpl impl) {
            if (impl.getPassword() == null || impl.getPassword().isBlank()) {
                log.error("[{}] SMTP authentication rejected: spring.mail.password is blank", correlationId);
                totalFailedCount.incrementAndGet();
                recentFailureCount.incrementAndGet();
                throw new EmailDeliveryException("We could not send the verification email. SMTP credentials unconfigured.");
            }
        }

        MimeMessage mimeMessage;
        try {
            mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(emailProperties.getFrom(), emailProperties.getSenderName());
            helper.setTo(safeTo);
            helper.setSubject(safeSubject);

            String plainText = message.getPlainTextContent() != null ? message.getPlainTextContent() : "";
            helper.setText(plainText, message.getHtmlContent());

            mimeMessage.setHeader("Auto-Submitted", "auto-generated");
            mimeMessage.setHeader("X-Auto-Response-Suppress", "All");
            mimeMessage.setHeader("X-Mailer", "HireAI-Notification-Service/2.0");
            mimeMessage.setHeader("X-Correlation-ID", correlationId);
            mimeMessage.setHeader("X-Email-Type", emailType);

        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error("[{}] Permanent failure constructing MIME message for {}: {}",
                    correlationId, EmailServiceImpl.maskEmail(safeTo), ex.getMessage());
            totalFailedCount.incrementAndGet();
            recentFailureCount.incrementAndGet();
            throw new EmailDeliveryException("We could not send the verification email. Please try Resend.", ex);
        }

        // Execution loop with exponential backoff on transient errors
        int maxRetries = Math.max(1, emailProperties.getMaxRetries());
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;
            try {
                mailSender.send(mimeMessage);
                totalSentCount.incrementAndGet();
                recentFailureCount.set(0);
                lastSuccessfulSend = Instant.now();
                log.info("[{}] Email ({}) successfully dispatched to {}",
                        correlationId, emailType, EmailServiceImpl.maskEmail(safeTo));
                return;

            } catch (Exception ex) {
                if (isPermanentFailure(ex)) {
                    log.error("[{}] Permanent SMTP failure sending {} to {}: {}",
                            correlationId, emailType, EmailServiceImpl.maskEmail(safeTo), ex.getMessage());
                    totalFailedCount.incrementAndGet();
                    recentFailureCount.incrementAndGet();
                    throw new EmailDeliveryException("We could not send the verification email. Please try Resend.", ex);
                }

                if (attempt < maxRetries) {
                    long backoffMs = emailProperties.getRetryDelayMs() * (1L << (attempt - 1));
                    log.warn("[{}] Transient SMTP failure (attempt {}/{}). Retrying in {}ms: {}",
                            correlationId, attempt, maxRetries, backoffMs, ex.getMessage());
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new EmailDeliveryException("Email dispatch interrupted", ie);
                    }
                } else {
                    log.error("[{}] Exhausted all {} retry attempts for {}: {}",
                            correlationId, maxRetries, EmailServiceImpl.maskEmail(safeTo), ex.getMessage());
                    totalFailedCount.incrementAndGet();
                    recentFailureCount.incrementAndGet();
                    throw new EmailDeliveryException("We could not send the verification email. Please try Resend.", ex);
                }
            }
        }
    }

    private boolean isPermanentFailure(Exception ex) {
        if (ex instanceof MailAuthenticationException) {
            return true;
        }
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof jakarta.mail.AuthenticationFailedException) {
                return true;
            }
            if (cause instanceof SendFailedException sendFailedEx) {
                if (sendFailedEx.getInvalidAddresses() != null && sendFailedEx.getInvalidAddresses().length > 0) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String sanitizeHeader(String header) {
        if (header == null) {
            return null;
        }
        return header.replace("\r", " ").replace("\n", " ").trim();
    }

    @Override
    public boolean isReady() {
        if (!emailProperties.isEnabled()) {
            return false;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return false;
        }
        if (mailSender instanceof JavaMailSenderImpl impl) {
            return impl.getPassword() != null && !impl.getPassword().isBlank();
        }
        return true;
    }

    @Override
    public EmailMode getMode() {
        return EmailMode.SMTP;
    }

    @Override
    public void checkConnection() {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender instanceof JavaMailSenderImpl impl) {
            try {
                impl.testConnection();
                log.info("SMTP connection check: Host {}:{} is reachable and accepting connections.",
                        impl.getHost(), impl.getPort());
            } catch (Exception ex) {
                log.error("SMTP connection check failed: {}", ex.getMessage());
                throw new EmailDeliveryException("SMTP connection check failed: " + ex.getMessage(), ex);
            }
        }
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
