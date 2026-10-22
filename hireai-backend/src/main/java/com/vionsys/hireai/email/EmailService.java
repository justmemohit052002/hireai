package com.vionsys.hireai.email;

import java.time.Instant;
import java.time.LocalDateTime;

import com.vionsys.hireai.application.dto.JobApplicationResponse;
import com.vionsys.hireai.application.enums.ApplicationStatus;
import com.vionsys.hireai.user.entity.User;

/**
 * Public service interface for technical recruitment notifications across HireAI.
 */
public interface EmailService {

    /**
     * Send automatic confirmation email to the candidate after submitting a job application.
     */
    void sendApplicationReceivedToCandidate(JobApplicationResponse application);

    /**
     * Send automatic alert email to the recruiter when a new candidate applies to their job.
     */
    void sendNewApplicantAlertToRecruiter(JobApplicationResponse application, String recruiterEmail);

    /**
     * Send status update email to the candidate when their recruitment pipeline stage changes.
     */
    void sendStatusUpdateToCandidate(JobApplicationResponse application, ApplicationStatus newStatus, String recruiterNotes);

    /**
     * Send welcome email to newly registered candidate.
     */
    void sendWelcomeCandidateEmail(User user);

    /**
     * Send welcome email to newly registered recruiter.
     */
    void sendWelcomeRecruiterEmail(User user);

    /**
     * Send security login alert email to user upon successful authentication.
     */
    void sendLoginAlertEmail(User user);

    /**
     * Send password reset token email to user.
     */
    void sendPasswordResetEmail(User user, String token, LocalDateTime expiryDate);

    /**
     * Send 6-digit email verification OTP to user.
     */
    void sendEmailVerificationOtp(User user, String otpCode, int expiryMinutes);

    /**
     * Send 6-digit OTP verification code to OAuth user requesting to set an account password.
     */
    void sendSetPasswordOtp(User user, String otpCode, int expiryMinutes);

    /**
     * Send 6-digit OTP verification code to OAuth user confirming account deletion.
     */
    void sendDeleteAccountOtp(User user, String otpCode, int expiryMinutes);

    /**
     * Send security notification when an account password is changed.
     */
    void sendPasswordChangedEmail(User user);

    /**
     * Send confirmation notification when an account is permanently deleted.
     */
    void sendAccountDeletedEmail(String email, String name);

    /**
     * Send confirmation email to recruiter when a new job posting is published.
     */
    void sendJobPostedConfirmation(String recruiterEmail, String recruiterName, String jobTitle, java.util.UUID jobId);

    /**
     * Send confirmation email to recruiter when a new job posting is published (legacy overload).
     */
    void sendJobPostedConfirmation(String recruiterEmail, String recruiterName, String jobTitle, Long jobId);

    /**
     * Send administrative notification when a sub-admin account is created.
     */
    void sendSubAdminCreatedEmail(String email, String createdByAdminEmail);

    /**
     * Send diagnostic test email to verify outbound email delivery.
     */
    void sendTestEmail(String toEmail);

    /**
     * Send generic HTML formatted email asynchronously.
     */
    void sendHtmlEmail(String toEmail, String subject, String htmlContent);

    /**
     * Returns true if SMTP outbound delivery is actively configured with valid credentials.
     */
    boolean isSmtpConfigured();

    /**
     * Returns the active delivery mode (SMTP or LOG).
     */
    EmailMode getEmailMode();

    /**
     * Timestamp of the most recent successful email dispatch.
     */
    Instant getLastSuccessfulSend();

    /**
     * Recent consecutive delivery failure count.
     */
    long getRecentFailureCount();

    /**
     * Mask email address for log hygiene (e.g. user@domain.com -> u***r@domain.com).
     */
    static String maskEmail(String email) {
        return EmailServiceImpl.maskEmail(email);
    }
}
