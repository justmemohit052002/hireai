package com.vionsys.hireai.email;

import com.vionsys.hireai.application.entity.JobApplication;
import com.vionsys.hireai.application.enums.ApplicationStatus;

public interface EmailService {

    /**
     * Send automatic confirmation email to the candidate after submitting a job application.
     */
    void sendApplicationReceivedToCandidate(JobApplication application);

    /**
     * Send automatic alert email to the recruiter when a new candidate applies to their job.
     */
    void sendNewApplicantAlertToRecruiter(JobApplication application);

    /**
     * Send status update email to the candidate when their recruitment pipeline stage changes.
     */
    void sendStatusUpdateToCandidate(JobApplication application, ApplicationStatus newStatus, String recruiterNotes);

    /**
     * Send welcome email to newly registered candidate.
     */
    void sendWelcomeCandidateEmail(com.vionsys.hireai.user.entity.User user);

    /**
     * Send welcome email to newly registered recruiter.
     */
    void sendWelcomeRecruiterEmail(com.vionsys.hireai.user.entity.User user);

    /**
     * Send security login alert email to user upon successful authentication.
     */
    void sendLoginAlertEmail(com.vionsys.hireai.user.entity.User user);

    /**
     * Send password reset token email to user.
     */
    void sendPasswordResetEmail(com.vionsys.hireai.user.entity.User user, String token, java.time.LocalDateTime expiryDate);

    /**
     * Send generic HTML formatted email asynchronously.
     */
    void sendHtmlEmail(String toEmail, String subject, String htmlContent);
}
