package com.vionsys.hireai.email;

import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.vionsys.hireai.application.dto.JobApplicationResponse;
import com.vionsys.hireai.application.enums.ApplicationStatus;
import com.vionsys.hireai.exception.EmailDeliveryException;
import com.vionsys.hireai.user.entity.User;

import lombok.extern.slf4j.Slf4j;

/**
 * Enterprise recruitment email service implementing transactional post-commit dispatch,
 * dual-mode transport routing (SMTP/LOG), dedicated thread pools, and template rendering.
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailProperties emailProperties;
    private final Environment environment;
    private final EmailTemplateRenderer templateRenderer;
    private final EmailDispatcher dispatcher;

    /**
     * Primary constructor for Spring dependency injection.
     */
    @Autowired
    public EmailServiceImpl(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            EmailProperties emailProperties,
            Environment environment,
            EmailTemplateRenderer templateRenderer,
            EmailDispatcher dispatcher) {
        this.mailSenderProvider = mailSenderProvider;
        this.emailProperties = emailProperties;
        this.environment = environment;
        this.templateRenderer = templateRenderer;
        this.dispatcher = dispatcher;
    }

    /**
     * Convenience constructor supporting isolated unit tests and standalone execution.
     */
    public EmailServiceImpl(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            EmailProperties emailProperties,
            Environment environment) {
        this.mailSenderProvider = mailSenderProvider;
        this.emailProperties = emailProperties;
        this.environment = environment;
        this.templateRenderer = new EmailTemplateRenderer();
        SmtpEmailTransport smtp = new SmtpEmailTransport(mailSenderProvider, emailProperties);
        LogEmailTransport logTransport = new LogEmailTransport(emailProperties, environment);
        this.dispatcher = new EmailDispatcher(smtp, logTransport, emailProperties);
    }

    // =========================================================================
    // 1. CANDIDATE: APPLICATION SUBMITTED CONFIRMATION
    // =========================================================================
    @Override
    public void sendApplicationReceivedToCandidate(JobApplicationResponse application) {
        if (!emailProperties.isEnabled() || application == null || application.getCandidateEmail() == null) {
            return;
        }

        EmailMessage message = templateRenderer.renderApplicationReceivedCandidate(
                application, emailProperties.getFrontendBaseUrl());
        dispatchAsyncSafe(message);
    }

    // =========================================================================
    // 2. RECRUITER: NEW APPLICANT ALERT
    // =========================================================================
    @Override
    public void sendNewApplicantAlertToRecruiter(JobApplicationResponse application, String recruiterEmail) {
        if (!emailProperties.isEnabled() || application == null || recruiterEmail == null || recruiterEmail.isBlank()) {
            return;
        }

        EmailMessage message = templateRenderer.renderNewApplicantAlertToRecruiter(
                application, recruiterEmail, emailProperties.getFrontendBaseUrl());
        dispatchAsyncSafe(message);
    }

    // =========================================================================
    // 3. CANDIDATE: RECRUITMENT STATUS UPDATE
    // =========================================================================
    @Override
    public void sendStatusUpdateToCandidate(JobApplicationResponse application, ApplicationStatus newStatus, String recruiterNotes) {
        if (!emailProperties.isEnabled() || application == null || application.getCandidateEmail() == null) {
            return;
        }

        EmailMessage message = templateRenderer.renderStatusUpdateCandidate(
                application, newStatus, recruiterNotes, emailProperties.getFrontendBaseUrl());
        dispatchAsyncSafe(message);
    }

    // =========================================================================
    // 4. CANDIDATE: WELCOME EMAIL
    // =========================================================================
    @Override
    public void sendWelcomeCandidateEmail(User user) {
        if (!emailProperties.isEnabled() || user == null || user.getEmail() == null) {
            return;
        }

        EmailMessage message = templateRenderer.renderWelcomeCandidate(user, emailProperties.getFrontendBaseUrl());
        dispatchAsyncSafe(message);
    }

    // =========================================================================
    // 5. RECRUITER: WELCOME EMAIL
    // =========================================================================
    @Override
    public void sendWelcomeRecruiterEmail(User user) {
        if (!emailProperties.isEnabled() || user == null || user.getEmail() == null) {
            return;
        }

        EmailMessage message = templateRenderer.renderWelcomeRecruiter(user, emailProperties.getFrontendBaseUrl());
        dispatchAsyncSafe(message);
    }

    // =========================================================================
    // 6. SECURITY: LOGIN ALERT
    // =========================================================================
    @Override
    public void sendLoginAlertEmail(User user) {
        if (!emailProperties.isEnabled() || user == null || user.getEmail() == null) {
            return;
        }

        EmailMessage message = templateRenderer.renderLoginAlert(user, emailProperties.getFrontendBaseUrl());
        dispatchAsyncSafe(message);
    }

    // =========================================================================
    // 7. USER: PASSWORD RESET TOKEN (CRITICAL AUTH)
    // =========================================================================
    @Override
    public void sendPasswordResetEmail(User user, String token, LocalDateTime expiryDate) {
        if (!emailProperties.isEnabled() || user == null || user.getEmail() == null) {
            return;
        }

        if (!isSmtpConfigured()) {
            if (emailProperties.getMode() == EmailMode.LOG || environment.acceptsProfiles(Profiles.of("dev", "test"))) {
                log.info("[SIMULATED EMAIL DISPATCH] (SMTP Unconfigured) Password Reset Token for {}: {}",
                        maskEmail(user.getEmail()), token);
                return;
            }
            throw new EmailDeliveryException("We could not send the password reset email. SMTP credentials unconfigured.");
        }

        EmailMessage message = templateRenderer.renderPasswordReset(
                user, token, expiryDate, emailProperties.getFrontendBaseUrl());
        dispatcher.dispatchSync(message);
    }

    // =========================================================================
    // 8. USER: 6-DIGIT EMAIL VERIFICATION OTP (CRITICAL AUTH)
    // =========================================================================
    @Override
    public void sendEmailVerificationOtp(User user, String otpCode, int expiryMinutes) {
        if (!emailProperties.isEnabled() || user == null || user.getEmail() == null) {
            return;
        }

        if (!isSmtpConfigured()) {
            if (emailProperties.getMode() == EmailMode.LOG || environment.acceptsProfiles(Profiles.of("dev", "test"))) {
                log.info("[SIMULATED EMAIL DISPATCH] (Dev Profile Active - SMTP Unconfigured) Verification OTP for {}: {}",
                        maskEmail(user.getEmail()), otpCode);
                return;
            }
            throw new EmailDeliveryException("We could not send the verification email. SMTP credentials unconfigured.");
        }

        EmailMessage message = templateRenderer.renderVerificationOtp(user, otpCode, expiryMinutes);
        dispatcher.dispatchSync(message);
    }

    // =========================================================================
    // 9. USER: SET PASSWORD OTP (CRITICAL AUTH)
    // =========================================================================
    @Override
    public void sendSetPasswordOtp(User user, String otpCode, int expiryMinutes) {
        if (!emailProperties.isEnabled() || user == null || user.getEmail() == null) {
            return;
        }

        if (!isSmtpConfigured()) {
            if (emailProperties.getMode() == EmailMode.LOG || environment.acceptsProfiles(Profiles.of("dev", "test"))) {
                log.info("[SIMULATED EMAIL DISPATCH] (Dev Profile Active - SMTP Unconfigured) Set Password OTP for {}: {}",
                        maskEmail(user.getEmail()), otpCode);
                return;
            }
            throw new EmailDeliveryException("We could not send the verification email. SMTP credentials unconfigured.");
        }

        EmailMessage message = templateRenderer.renderSetPasswordOtp(user, otpCode, expiryMinutes);
        dispatcher.dispatchSync(message);
    }

    // =========================================================================
    // 10. USER: DELETE ACCOUNT OTP (CRITICAL AUTH)
    // =========================================================================
    @Override
    public void sendDeleteAccountOtp(User user, String otpCode, int expiryMinutes) {
        if (!emailProperties.isEnabled() || user == null || user.getEmail() == null) {
            return;
        }

        if (!isSmtpConfigured()) {
            if (emailProperties.getMode() == EmailMode.LOG || environment.acceptsProfiles(Profiles.of("dev", "test"))) {
                log.info("[SIMULATED EMAIL DISPATCH] (Dev Profile Active - SMTP Unconfigured) Delete Account OTP for {}: {}",
                        maskEmail(user.getEmail()), otpCode);
                return;
            }
            throw new EmailDeliveryException("We could not send the verification email. SMTP credentials unconfigured.");
        }

        EmailMessage message = templateRenderer.renderDeleteAccountOtp(user, otpCode, expiryMinutes);
        dispatcher.dispatchSync(message);
    }

    // =========================================================================
    // 11. SECURITY: PASSWORD CHANGED NOTIFICATION
    // =========================================================================
    @Override
    public void sendPasswordChangedEmail(User user) {
        if (!emailProperties.isEnabled() || user == null || user.getEmail() == null) {
            return;
        }

        EmailMessage message = templateRenderer.renderPasswordChanged(user, emailProperties.getFrontendBaseUrl());
        dispatchAsyncSafe(message);
    }

    // =========================================================================
    // 12. SECURITY: ACCOUNT DELETED NOTIFICATION
    // =========================================================================
    @Override
    public void sendAccountDeletedEmail(String email, String name) {
        if (!emailProperties.isEnabled() || email == null || email.isBlank()) {
            return;
        }

        EmailMessage message = templateRenderer.renderAccountDeleted(email, name);
        dispatchAsyncSafe(message);
    }

    // =========================================================================
    // 13. RECRUITER: JOB POSTED CONFIRMATION
    // =========================================================================
    @Override
    public void sendJobPostedConfirmation(String recruiterEmail, String recruiterName, String jobTitle, java.util.UUID jobId) {
        if (!emailProperties.isEnabled() || recruiterEmail == null || recruiterEmail.isBlank()) {
            return;
        }

        EmailMessage message = templateRenderer.renderJobPostedConfirmation(
                recruiterEmail, recruiterName, jobTitle, jobId, emailProperties.getFrontendBaseUrl());
        dispatchAsyncSafe(message);
    }

    @Override
    public void sendJobPostedConfirmation(String recruiterEmail, String recruiterName, String jobTitle, Long jobId) {
        if (!emailProperties.isEnabled() || recruiterEmail == null || recruiterEmail.isBlank()) {
            return;
        }

        EmailMessage message = templateRenderer.renderJobPostedConfirmation(
                recruiterEmail, recruiterName, jobTitle, jobId, emailProperties.getFrontendBaseUrl());
        dispatchAsyncSafe(message);
    }

    // =========================================================================
    // 14. ADMIN: SUB-ADMIN CREATED NOTIFICATION
    // =========================================================================
    @Override
    public void sendSubAdminCreatedEmail(String email, String createdByAdminEmail) {
        if (!emailProperties.isEnabled() || email == null || email.isBlank()) {
            return;
        }

        EmailMessage message = templateRenderer.renderSubAdminCreated(
                email, createdByAdminEmail, emailProperties.getFrontendBaseUrl());
        dispatchAsyncSafe(message);
    }

    // =========================================================================
    // 15. DIAGNOSTICS: TEST EMAIL
    // =========================================================================
    @Override
    public void sendTestEmail(String toEmail) {
        if (!emailProperties.isEnabled() || toEmail == null || toEmail.isBlank()) {
            return;
        }

        EmailMessage message = templateRenderer.renderTestEmail(toEmail, emailProperties.getFrontendBaseUrl());
        dispatcher.dispatchSync(message);
    }

    // =========================================================================
    // 16. GENERIC HTML DISPATCH
    // =========================================================================
    @Override
    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        if (!emailProperties.isEnabled() || toEmail == null || toEmail.isBlank()) {
            return;
        }

        if (!isSmtpConfigured()) {
            log.info("[SIMULATED EMAIL DISPATCH] (SMTP unconfigured or hireai.mail.enabled=false)\n"
                            + "==================================================\n"
                            + "TO: {}\n"
                            + "FROM: {} <{}>\n"
                            + "SUBJECT: {}\n"
                            + "==================================================",
                    maskEmail(toEmail), emailProperties.getSenderName(), emailProperties.getFrom(), subject);
            return;
        }

        EmailMessage message = templateRenderer.renderGenericHtml(toEmail, subject, htmlContent);
        dispatcher.dispatchSync(message);
    }

    // =========================================================================
    // OBSERVABILITY & STATUS
    // =========================================================================
    @Override
    public boolean isSmtpConfigured() {
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
    public EmailMode getEmailMode() {
        return emailProperties.getMode();
    }

    @Override
    public Instant getLastSuccessfulSend() {
        return dispatcher.resolveTransport().getLastSuccessfulSend();
    }

    @Override
    public long getRecentFailureCount() {
        return dispatcher.resolveTransport().getRecentFailureCount();
    }

    // =========================================================================
    // INTERNAL TRANSACTION & LOG HYGIENE HELPERS
    // =========================================================================
    private void dispatchAsyncSafe(EmailMessage message) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatcher.dispatchAsync(message);
                }
            });
        } else {
            dispatcher.dispatchAsync(message);
        }
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + (atIndex >= 0 ? email.substring(atIndex) : "");
        }
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "*" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }
}
