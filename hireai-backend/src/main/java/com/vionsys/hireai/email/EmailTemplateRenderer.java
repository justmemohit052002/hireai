package com.vionsys.hireai.email;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import com.vionsys.hireai.application.dto.JobApplicationResponse;
import com.vionsys.hireai.application.enums.ApplicationStatus;
import com.vionsys.hireai.user.entity.User;

/**
 * Dedicated template rendering service producing high-fidelity, brand-compliant HTML and plain-text email bodies.
 * Strictly adheres to HireAI design directives: official brand blues (#004499, #0192C6, #112358), neutral slates,
 * rounded-lg borders, no purple hexes, no emojis, no em dashes, and strict HTML escaping for all user inputs.
 */
@Component
public class EmailTemplateRenderer {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // =========================================================================
    // 1. CANDIDATE: APPLICATION RECEIVED CONFIRMATION
    // =========================================================================
    public EmailMessage renderApplicationReceivedCandidate(JobApplicationResponse app, String frontendBaseUrl) {
        String candidateEmail = app.getCandidateEmail();
        String candidateName = escape(app.getCandidateName() != null ? app.getCandidateName() : "Candidate");
        String jobTitle = escape(app.getJobTitle() != null ? app.getJobTitle() : "Job Opening");
        String companyName = escape(app.getCompanyName() != null ? app.getCompanyName() : "HireAI Partner");
        int score = app.getAtsMatchScore() != null ? app.getAtsMatchScore().intValue() : 0;
        ApplicationStatus status = app.getStatus() != null ? app.getStatus() : ApplicationStatus.APPLIED;

        String badgeColor = status == ApplicationStatus.SHORTLISTED ? "#10b981" : (status == ApplicationStatus.REJECTED ? "#ef4444" : "#0192C6");
        String statusText = status == ApplicationStatus.SHORTLISTED ? "SHORTLISTED FOR INTERVIEW"
                : (status == ApplicationStatus.REJECTED ? "NOT SELECTED" : "UNDER REVIEW");

        String dashboardUrl = sanitizeUrl(frontendBaseUrl) + "/candidate/applications";
        String subject = String.format("Application Received: %s at %s", jobTitle, companyName);

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #112358, #004499); padding: 30px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px; }
                .header p { margin: 8px 0 0; color: #94a3b8; font-size: 14px; }
                .content { padding: 32px 28px; }
                .greeting { font-size: 15px; margin-bottom: 20px; line-height: 1.6; }
                .card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; margin: 24px 0; }
                .card-row { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 14px; }
                .card-row:last-child { margin-bottom: 0; }
                .label { color: #64748b; font-weight: 500; }
                .value { font-weight: 600; color: #0f172a; }
                .badge { display: inline-block; padding: 6px 14px; border-radius: 8px; font-size: 12px; font-weight: 700; color: #ffffff; background: %s; }
                .btn { display: inline-block; padding: 12px 24px; background: #004499; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 14px; margin-top: 15px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>HireAI</h1>
                  <p>Intelligent Recruitment and Talent Acceleration</p>
                </div>
                <div class="content">
                  <div class="greeting">
                    Dear <strong>%s</strong>,<br><br>
                    Thank you for applying for the position of <strong>%s</strong> at <strong>%s</strong>. We have received your application and processed your profile through our recruitment matching system.
                  </div>
                  <div class="card">
                    <div class="card-row"><span class="label">Job Title:</span><span class="value">%s</span></div>
                    <div class="card-row"><span class="label">Company:</span><span class="value">%s</span></div>
                    <div class="card-row"><span class="label">Initial Status:</span><span class="badge">%s</span></div>
                    <div class="card-row"><span class="label">ATS Match Score:</span><span class="value">%d%%</span></div>
                  </div>
                  <p style="font-size: 14px; color: #475569; line-height: 1.6;">
                    The recruiting team will review your application. You can monitor the progress of your application live in your candidate dashboard:
                  </p>
                  <p style="text-align: center;"><a href="%s" class="btn" style="color:#ffffff;">View Application Status</a></p>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Platform. All rights reserved.<br>
                  This is an automated notification. Please do not reply directly to this email.
                </div>
              </div>
            </body>
            </html>
            """, badgeColor, candidateName, jobTitle, companyName, jobTitle, companyName, statusText, score, dashboardUrl);

        String plainText = String.format("""
            Dear %s,

            Thank you for applying for the position of %s at %s. We have received your application.

            Job Title: %s
            Company: %s
            Initial Status: %s
            ATS Match Score: %d%%

            You can track your application live at: %s

            (c) 2026 HireAI Platform. All rights reserved.
            """, candidateName, jobTitle, companyName, jobTitle, companyName, statusText, score, dashboardUrl);

        return EmailMessage.builder()
                .to(candidateEmail)
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("APPLICATION_RECEIVED_CANDIDATE")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    // =========================================================================
    // 2. RECRUITER: NEW APPLICANT ALERT
    // =========================================================================
    public EmailMessage renderNewApplicantAlertToRecruiter(JobApplicationResponse app, String recruiterEmail, String frontendBaseUrl) {
        String candidateName = escape(app.getCandidateName() != null ? app.getCandidateName() : "Candidate");
        String candidateEmail = escape(app.getCandidateEmail() != null ? app.getCandidateEmail() : "");
        String jobTitle = escape(app.getJobTitle() != null ? app.getJobTitle() : "Job Position");
        String companyName = escape(app.getCompanyName() != null ? app.getCompanyName() : "HireAI Partner");
        int score = app.getAtsMatchScore() != null ? app.getAtsMatchScore().intValue() : 0;
        String applicantUrl = sanitizeUrl(frontendBaseUrl) + "/recruiter/applications";
        String subject = String.format("New Candidate Application: %s for %s", candidateName, jobTitle);

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #112358, #004499); padding: 30px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 24px; font-weight: 700; }
                .header p { margin: 8px 0 0; color: #94a3b8; font-size: 14px; }
                .content { padding: 32px 28px; }
                .card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; margin: 24px 0; }
                .card-row { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 14px; }
                .card-row:last-child { margin-bottom: 0; }
                .label { color: #64748b; font-weight: 500; }
                .value { font-weight: 600; color: #0f172a; }
                .score-box { text-align: center; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 16px; margin: 20px 0; }
                .score-num { font-size: 28px; font-weight: 800; color: #10b981; }
                .btn { display: inline-block; padding: 12px 24px; background: #004499; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 14px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>HireAI</h1>
                  <p>Recruiter Talent Alert</p>
                </div>
                <div class="content">
                  <p style="font-size: 15px; line-height: 1.6;">
                    A new candidate has submitted an application for your active job posting: <strong>%s</strong> at <strong>%s</strong>.
                  </p>
                  <div class="card">
                    <div class="card-row"><span class="label">Candidate Name:</span><span class="value">%s</span></div>
                    <div class="card-row"><span class="label">Candidate Email:</span><span class="value">%s</span></div>
                    <div class="card-row"><span class="label">Job Title:</span><span class="value">%s</span></div>
                    <div class="card-row"><span class="label">Company:</span><span class="value">%s</span></div>
                  </div>
                  <div class="score-box">
                    <div style="font-size: 13px; color: #166534; font-weight: 600;">Automated ATS Compatibility Match</div>
                    <div class="score-num">%d%%</div>
                  </div>
                  <p style="text-align: center;"><a href="%s" class="btn" style="color:#ffffff;">Review Candidate Profile</a></p>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Platform. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """, jobTitle, companyName, candidateName, candidateEmail, jobTitle, companyName, score, applicantUrl);

        String plainText = String.format("""
            New Candidate Application Received for %s

            Candidate Name: %s
            Candidate Email: %s
            Job Title: %s
            ATS Match Score: %d%%

            Review profile: %s

            (c) 2026 HireAI Platform.
            """, jobTitle, candidateName, candidateEmail, jobTitle, score, applicantUrl);

        return EmailMessage.builder()
                .to(recruiterEmail)
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("NEW_APPLICANT_RECRUITER")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    // =========================================================================
    // 3. CANDIDATE: APPLICATION STATUS UPDATE
    // =========================================================================
    public EmailMessage renderStatusUpdateCandidate(JobApplicationResponse app, ApplicationStatus newStatus, String recruiterNotes, String frontendBaseUrl) {
        String candidateEmail = app.getCandidateEmail();
        String candidateName = escape(app.getCandidateName() != null ? app.getCandidateName() : "Candidate");
        String jobTitle = escape(app.getJobTitle() != null ? app.getJobTitle() : "Job Position");
        String companyName = escape(app.getCompanyName() != null ? app.getCompanyName() : "HireAI Partner");

        String badgeColor = newStatus == ApplicationStatus.OFFERED ? "#10b981"
                : (newStatus == ApplicationStatus.INTERVIEW_SCHEDULED ? "#004499"
                : (newStatus == ApplicationStatus.SHORTLISTED ? "#0192C6"
                : (newStatus == ApplicationStatus.REJECTED ? "#ef4444" : "#f59e0b")));

        String statusLabel = newStatus.name().replace('_', ' ');
        String dashboardUrl = sanitizeUrl(frontendBaseUrl) + "/candidate/applications";
        String subject = String.format("Application Status Update: %s at %s", jobTitle, companyName);

        String notesHtml = (recruiterNotes != null && !recruiterNotes.isBlank())
                ? String.format("<div style=\"background: #f8fafc; border-left: 4px solid #004499; padding: 14px 18px; border-radius: 4px; font-size: 13px; color: #1e293b; margin: 20px 0;\"><strong>Recruiter Note:</strong> %s</div>", escape(recruiterNotes))
                : "";

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #112358, #004499); padding: 30px; text-align: center; color: #ffffff; }
                .content { padding: 32px 28px; }
                .badge { display: inline-block; padding: 8px 18px; border-radius: 8px; font-size: 14px; font-weight: 700; color: #ffffff; background: %s; }
                .btn { display: inline-block; padding: 12px 24px; background: #004499; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 14px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1 style="margin:0; font-size:24px;">HireAI Status Notification</h1>
                </div>
                <div class="content">
                  <p style="font-size: 15px;">Hello <strong>%s</strong>,</p>
                  <p style="font-size: 14px; color: #475569; line-height: 1.6;">
                    The status of your application for <strong>%s</strong> at <strong>%s</strong> has been updated:
                  </p>
                  <div style="text-align: center; margin: 24px 0;">
                    <span class="badge">%s</span>
                  </div>
                  %s
                  <p style="text-align: center; margin-top: 25px;">
                    <a href="%s" class="btn" style="color:#ffffff;">View Application Details</a>
                  </p>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Platform. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """, badgeColor, candidateName, jobTitle, companyName, statusLabel, notesHtml, dashboardUrl);

        String plainText = String.format("""
            Hello %s,

            Your application status for %s at %s has been updated to: %s.

            %s
            View details: %s

            (c) 2026 HireAI Platform.
            """, candidateName, jobTitle, companyName, statusLabel,
                (recruiterNotes != null && !recruiterNotes.isBlank() ? "Note: " + recruiterNotes + "\n" : ""),
                dashboardUrl);

        return EmailMessage.builder()
                .to(candidateEmail)
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("STATUS_UPDATE_CANDIDATE")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    // =========================================================================
    // 4. USER: 6-DIGIT EMAIL VERIFICATION OTP
    // =========================================================================
    public EmailMessage renderVerificationOtp(User user, String otpCode, int expiryMinutes) {
        String name = escape(formatName(user));
        String subject = "Verify Your Email Address - HireAI";

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #0f172a; margin: 0; padding: 24px; color: #f8fafc; }
                .container { max-width: 580px; margin: 0 auto; background: #1e293b; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.4); border: 1px solid #334155; }
                .header { background: linear-gradient(135deg, #112358, #004499); padding: 32px 24px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 24px; font-weight: 700; }
                .header p { margin: 6px 0 0; font-size: 14px; opacity: 0.9; }
                .content { padding: 32px 28px; }
                .greeting { font-size: 15px; line-height: 1.6; color: #cbd5e1; margin-bottom: 20px; }
                .otp-box { background: #0f172a; border: 2px dashed #0192C6; border-radius: 8px; padding: 24px; text-align: center; margin: 28px 0; }
                .otp-label { font-size: 12px; text-transform: uppercase; letter-spacing: 1.5px; font-weight: 600; color: #0192C6; margin-bottom: 12px; }
                .otp-code { font-family: 'Courier New', monospace; font-size: 36px; font-weight: 800; letter-spacing: 10px; color: #ffffff; }
                .security-note { background: rgba(245, 158, 11, 0.1); border-left: 4px solid #f59e0b; padding: 12px 16px; border-radius: 6px; font-size: 12px; color: #fcd34d; margin-top: 24px; }
                .footer { background: #0b0f19; padding: 20px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #1e293b; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>HireAI Email Verification</h1>
                  <p>Confirm your email to complete registration</p>
                </div>
                <div class="content">
                  <p class="greeting">Hello <strong>%s</strong>,</p>
                  <p class="greeting">Thank you for registering on <strong>HireAI</strong>. To activate your account and start using the platform, enter the 6-digit verification code below:</p>
                  <div class="otp-box">
                    <div class="otp-label">Verification Code</div>
                    <div class="otp-code">%s</div>
                  </div>
                  <p style="font-size: 13px; color: #94a3b8; line-height: 1.6;">
                    This verification code is valid for <strong>%d minutes</strong>.
                  </p>
                  <div class="security-note">
                    <strong>Security Tip:</strong> Never share this code with anyone. HireAI staff will never ask for your verification code.
                  </div>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Intelligent Recruitment Platform. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """, name, otpCode, expiryMinutes);

        String plainText = String.format("""
            Hello %s,

            Thank you for registering on HireAI. Your 6-digit verification code is:

            %s

            This code expires in %d minutes. Never share this code with anyone.

            (c) 2026 HireAI Platform.
            """, name, otpCode, expiryMinutes);

        return EmailMessage.builder()
                .to(user.getEmail())
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("VERIFICATION_OTP")
                .correlationId(UUID.randomUUID().toString())
                .sensitiveToken(otpCode)
                .build();
    }

    // =========================================================================
    // 5. USER: SET PASSWORD OTP
    // =========================================================================
    public EmailMessage renderSetPasswordOtp(User user, String otpCode, int expiryMinutes) {
        String name = escape(formatName(user));
        String subject = "Set Account Password - Verification Code - HireAI";

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #0f172a; margin: 0; padding: 24px; color: #f8fafc; }
                .container { max-width: 580px; margin: 0 auto; background: #1e293b; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.4); border: 1px solid #334155; }
                .header { background: linear-gradient(135deg, #112358, #004499); padding: 32px 24px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 24px; font-weight: 700; }
                .content { padding: 32px 28px; }
                .otp-box { background: #0f172a; border: 2px dashed #0192C6; border-radius: 8px; padding: 24px; text-align: center; margin: 28px 0; }
                .otp-label { font-size: 12px; text-transform: uppercase; letter-spacing: 1.5px; font-weight: 600; color: #0192C6; margin-bottom: 12px; }
                .otp-code { font-family: 'Courier New', monospace; font-size: 36px; font-weight: 800; letter-spacing: 10px; color: #ffffff; }
                .footer { background: #0b0f19; padding: 20px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #1e293b; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Set Account Password</h1>
                  <p style="margin:6px 0 0; font-size:14px; opacity:0.9;">HireAI Security Service</p>
                </div>
                <div class="content">
                  <p style="font-size:15px; color:#cbd5e1;">Hello %s,</p>
                  <p style="font-size:14px; color:#94a3b8; line-height:1.6;">
                    You requested to set a password for your HireAI account. Use the 6-digit verification code below:
                  </p>
                  <div class="otp-box">
                    <div class="otp-label">Verification Code</div>
                    <div class="otp-code">%s</div>
                  </div>
                  <p style="font-size:13px; color:#94a3b8;">This code is valid for <strong>%d minutes</strong>.</p>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Intelligent Recruitment Platform.
                </div>
              </div>
            </body>
            </html>
            """, name, otpCode, expiryMinutes);

        String plainText = String.format("""
            Hello %s,

            Your 6-digit verification code to set your account password is: %s (expires in %d minutes).

            (c) 2026 HireAI Platform.
            """, name, otpCode, expiryMinutes);

        return EmailMessage.builder()
                .to(user.getEmail())
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("SET_PASSWORD_OTP")
                .correlationId(UUID.randomUUID().toString())
                .sensitiveToken(otpCode)
                .build();
    }

    // =========================================================================
    // 6. USER: DELETE ACCOUNT OTP
    // =========================================================================
    public EmailMessage renderDeleteAccountOtp(User user, String otpCode, int expiryMinutes) {
        String name = escape(formatName(user));
        String subject = "Confirm Account Deletion - Verification Code - HireAI";

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #0f172a; margin: 0; padding: 24px; color: #f8fafc; }
                .container { max-width: 580px; margin: 0 auto; background: #1e293b; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.4); border: 1px solid #ef4444; }
                .header { background: linear-gradient(135deg, #7f1d1d, #991b1b); padding: 32px 24px; text-align: center; color: #ffffff; }
                .content { padding: 32px 28px; }
                .otp-box { background: #0f172a; border: 2px dashed #ef4444; border-radius: 8px; padding: 24px; text-align: center; margin: 28px 0; }
                .otp-label { font-size: 12px; text-transform: uppercase; letter-spacing: 1.5px; font-weight: 600; color: #ef4444; margin-bottom: 12px; }
                .otp-code { font-family: 'Courier New', monospace; font-size: 36px; font-weight: 800; letter-spacing: 10px; color: #ffffff; }
                .footer { background: #0b0f19; padding: 20px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #1e293b; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1 style="margin:0; font-size:24px;">Confirm Account Deletion</h1>
                  <p style="margin:6px 0 0; font-size:14px; opacity:0.9;">Permanent Account Removal Request</p>
                </div>
                <div class="content">
                  <p style="font-size:15px; color:#cbd5e1;">Hello %s,</p>
                  <p style="font-size:14px; color:#f87171; line-height:1.6;">
                    A request was submitted to permanently delete your HireAI account. If you wish to confirm this deletion, use the verification code below:
                  </p>
                  <div class="otp-box">
                    <div class="otp-label">Deletion Confirmation Code</div>
                    <div class="otp-code">%s</div>
                  </div>
                  <p style="font-size:13px; color:#94a3b8;">This code is valid for <strong>%d minutes</strong>. If you did not request this, please secure your account immediately.</p>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Intelligent Recruitment Platform.
                </div>
              </div>
            </body>
            </html>
            """, name, otpCode, expiryMinutes);

        String plainText = String.format("""
            Hello %s,

            Your deletion confirmation code is: %s (expires in %d minutes).
            If you did not initiate this request, change your password immediately.

            (c) 2026 HireAI Platform.
            """, name, otpCode, expiryMinutes);

        return EmailMessage.builder()
                .to(user.getEmail())
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("DELETE_ACCOUNT_OTP")
                .correlationId(UUID.randomUUID().toString())
                .sensitiveToken(otpCode)
                .build();
    }

    // =========================================================================
    // 7. USER: PASSWORD RESET TOKEN
    // =========================================================================
    public EmailMessage renderPasswordReset(User user, String token, LocalDateTime expiryDate, String frontendBaseUrl) {
        String name = escape(formatName(user));
        String expiryStr = expiryDate != null ? expiryDate.format(DATE_FORMATTER) : "in 15 minutes";
        String resetUrl = sanitizeUrl(frontendBaseUrl) + "/reset-password?token=" + token;
        String subject = "Reset Your HireAI Account Password";

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #112358, #004499); padding: 30px; text-align: center; color: #ffffff; }
                .content { padding: 32px 28px; }
                .token-box { background: #f8fafc; border: 2px dashed #0192C6; border-radius: 8px; padding: 18px; text-align: center; margin: 24px 0; }
                .token-code { font-family: monospace; font-size: 16px; font-weight: bold; color: #004499; word-break: break-all; }
                .btn { display: inline-block; padding: 12px 24px; background: #004499; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 14px; margin-top: 15px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h2 style="margin:0; font-size:24px;">Password Reset Request</h2>
                </div>
                <div class="content">
                  <p style="font-size:15px;">Hello <strong>%s</strong>,</p>
                  <p style="font-size:14px; color:#475569; line-height:1.6;">
                    We received a request to reset the password for your HireAI account. Click the button below or copy the token to reset your password:
                  </p>
                  <div class="token-box">
                    <p style="margin:0 0 8px 0; font-size:12px; color:#64748b;"><strong>Secure Reset Token:</strong></p>
                    <div class="token-code">%s</div>
                  </div>
                  <p style="text-align: center;">
                    <a href="%s" class="btn" style="color:#ffffff;">Reset Password Online</a>
                  </p>
                  <p style="font-size: 13px; color: #64748b; margin-top: 20px;">
                    This token is valid until <strong>%s</strong>. If you did not request a password reset, you can safely ignore this email.
                  </p>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Platform. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """, name, token, resetUrl, expiryStr);

        String plainText = String.format("""
            Hello %s,

            We received a request to reset your password. Use the token below:
            Token: %s
            Reset URL: %s
            Expires: %s

            If you did not request this, please ignore this email.

            (c) 2026 HireAI Platform.
            """, name, token, resetUrl, expiryStr);

        return EmailMessage.builder()
                .to(user.getEmail())
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("PASSWORD_RESET")
                .correlationId(UUID.randomUUID().toString())
                .sensitiveToken(token)
                .build();
    }

    // =========================================================================
    // 8. SECURITY: LOGIN ALERT
    // =========================================================================
    public EmailMessage renderLoginAlert(User user, String frontendBaseUrl) {
        String name = escape(formatName(user));
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String settingsUrl = sanitizeUrl(frontendBaseUrl) + "/settings";
        String subject = "Security Alert: New Sign-In to Your HireAI Account";

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #112358, #004499); padding: 30px; text-align: center; color: #ffffff; }
                .content { padding: 32px 28px; }
                .card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; margin: 24px 0; }
                .card-row { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 14px; }
                .card-row:last-child { margin-bottom: 0; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h2 style="margin:0; font-size:24px;">Security Alert: Account Sign-In</h2>
                </div>
                <div class="content">
                  <p style="font-size:15px;">Hello <strong>%s</strong>,</p>
                  <p style="font-size:14px; color:#475569; line-height:1.6;">
                    A new sign-in was detected on your HireAI account. Details of the session:
                  </p>
                  <div class="card">
                    <div class="card-row"><span style="color:#64748b;">Account Email:</span><span style="font-weight:600;">%s</span></div>
                    <div class="card-row"><span style="color:#64748b;">Timestamp:</span><span style="font-weight:600;">%s</span></div>
                  </div>
                  <p style="font-size:13px; color:#64748b; line-height:1.6;">
                    If this was you, no action is required. If you did not sign in recently, please secure your account immediately:
                  </p>
                  <p style="text-align: center; margin-top:20px;">
                    <a href="%s" style="display:inline-block; padding:12px 24px; background:#004499; color:#ffffff; text-decoration:none; border-radius:8px; font-weight:600; font-size:14px;">Review Account Security</a>
                  </p>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Platform. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """, name, escape(user.getEmail()), timestamp, settingsUrl);

        String plainText = String.format("""
            Hello %s,

            A new sign-in was detected on your HireAI account (%s) at %s.
            If you did not perform this login, review your security settings at: %s

            (c) 2026 HireAI Platform.
            """, name, user.getEmail(), timestamp, settingsUrl);

        return EmailMessage.builder()
                .to(user.getEmail())
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("LOGIN_SECURITY_ALERT")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    // =========================================================================
    // 9. SECURITY: PASSWORD CHANGED CONFIRMATION
    // =========================================================================
    public EmailMessage renderPasswordChanged(User user, String frontendBaseUrl) {
        String name = escape(formatName(user));
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String forgotUrl = sanitizeUrl(frontendBaseUrl) + "/forgot-password";
        String subject = "Security Alert: Password Changed - HireAI";

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #112358, #004499); padding: 30px; text-align: center; color: #ffffff; }
                .content { padding: 32px 28px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h2 style="margin:0; font-size:24px;">Password Successfully Updated</h2>
                </div>
                <div class="content">
                  <p style="font-size:15px;">Hello <strong>%s</strong>,</p>
                  <p style="font-size:14px; color:#475569; line-height:1.6;">
                    The password for your HireAI account was successfully changed on <strong>%s</strong>.
                  </p>
                  <p style="font-size:13px; color:#ef4444; line-height:1.6; margin-top:20px;">
                    <strong>Important:</strong> If you did not make this change, your account may be compromised. Please reset your password immediately:
                  </p>
                  <p style="text-align: center; margin-top:20px;">
                    <a href="%s" style="display:inline-block; padding:12px 24px; background:#ef4444; color:#ffffff; text-decoration:none; border-radius:8px; font-weight:600; font-size:14px;">Secure Your Account</a>
                  </p>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Platform. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """, name, timestamp, forgotUrl);

        String plainText = String.format("""
            Hello %s,

            Your HireAI account password was changed on %s.
            If you did not perform this action, reset your password immediately at: %s

            (c) 2026 HireAI Platform.
            """, name, timestamp, forgotUrl);

        return EmailMessage.builder()
                .to(user.getEmail())
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("PASSWORD_CHANGED")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    // =========================================================================
    // 10. SECURITY: ACCOUNT DELETED CONFIRMATION
    // =========================================================================
    public EmailMessage renderAccountDeleted(String email, String name) {
        String safeName = escape(name != null && !name.isBlank() ? name : "User");
        String subject = "HireAI Account Deletion Confirmed";

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #112358, #004499); padding: 30px; text-align: center; color: #ffffff; }
                .content { padding: 32px 28px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h2 style="margin:0; font-size:24px;">Account Deletion Confirmation</h2>
                </div>
                <div class="content">
                  <p style="font-size:15px;">Hello <strong>%s</strong>,</p>
                  <p style="font-size:14px; color:#475569; line-height:1.6;">
                    Your HireAI account and associated profile data have been permanently removed and anonymized according to your request.
                  </p>
                  <p style="font-size:13px; color:#64748b; line-height:1.6; margin-top:20px;">
                    Thank you for using HireAI. We wish you continued success in your professional endeavors.
                  </p>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Platform. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """, safeName);

        String plainText = String.format("""
            Hello %s,

            Your HireAI account and profile data have been successfully deleted and anonymized.
            Thank you for being part of our community.

            (c) 2026 HireAI Platform.
            """, safeName);

        return EmailMessage.builder()
                .to(email)
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("ACCOUNT_DELETED")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    // =========================================================================
    // 11. RECRUITER: JOB POSTED CONFIRMATION
    // =========================================================================
    public EmailMessage renderJobPostedConfirmation(String recruiterEmail, String recruiterName, String jobTitle, Object jobId, String frontendBaseUrl) {
        String safeName = escape(recruiterName != null && !recruiterName.isBlank() ? recruiterName : "Recruiter");
        String safeTitle = escape(jobTitle != null && !jobTitle.isBlank() ? jobTitle : "New Job Position");
        String jobUrl = sanitizeUrl(frontendBaseUrl) + "/recruiter/jobs";
        String subject = String.format("Job Posting Live: %s - HireAI", safeTitle);

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #112358, #004499); padding: 30px; text-align: center; color: #ffffff; }
                .content { padding: 32px 28px; }
                .btn { display: inline-block; padding: 12px 24px; background: #004499; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 14px; margin-top: 15px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h2 style="margin:0; font-size:24px;">Job Posting Published</h2>
                </div>
                <div class="content">
                  <p style="font-size:15px;">Hello <strong>%s</strong>,</p>
                  <p style="font-size:14px; color:#475569; line-height:1.6;">
                    Your new job posting <strong>%s</strong> is now active and published on the HireAI platform. Candidates will begin receiving automated recommendations based on ATS skill indexing.
                  </p>
                  <p style="text-align: center; margin-top:25px;">
                    <a href="%s" class="btn" style="color:#ffffff;">Manage Job Posting</a>
                  </p>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Platform. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """, safeName, safeTitle, jobUrl);

        String plainText = String.format("""
            Hello %s,

            Your job posting '%s' is now published and active on HireAI.
            Manage your posting at: %s

            (c) 2026 HireAI Platform.
            """, safeName, safeTitle, jobUrl);

        return EmailMessage.builder()
                .to(recruiterEmail)
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("JOB_POSTED_CONFIRMATION")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    // =========================================================================
    // 12. WELCOME CANDIDATE
    // =========================================================================
    public EmailMessage renderWelcomeCandidate(User user, String frontendBaseUrl) {
        String name = escape(formatName(user));
        String jobsUrl = sanitizeUrl(frontendBaseUrl) + "/candidate/jobs";
        String subject = "Welcome to HireAI - Accelerate Your Tech Career";

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #112358, #004499); padding: 30px; text-align: center; color: #ffffff; }
                .content { padding: 32px 28px; }
                .btn { display: inline-block; padding: 12px 24px; background: #004499; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 14px; margin-top: 15px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1 style="margin:0; font-size:24px;">Welcome to HireAI</h1>
                  <p style="margin:8px 0 0; font-size:14px; color:#94a3b8;">Deterministic ATS Matching & Career Acceleration</p>
                </div>
                <div class="content">
                  <p style="font-size:15px;">Welcome <strong>%s</strong>,</p>
                  <p style="font-size:14px; color:#475569; line-height:1.6;">
                    Thank you for joining HireAI. Our platform pairs your technical skill matrix with verified engineering opportunities.
                  </p>
                  <p style="text-align: center; margin-top:25px;">
                    <a href="%s" class="btn" style="color:#ffffff;">Explore Open Roles</a>
                  </p>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Platform. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """, name, jobsUrl);

        String plainText = String.format("""
            Welcome to HireAI, %s!
            Explore matching engineering roles at: %s

            (c) 2026 HireAI Platform.
            """, name, jobsUrl);

        return EmailMessage.builder()
                .to(user.getEmail())
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("WELCOME_CANDIDATE")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    // =========================================================================
    // 13. WELCOME RECRUITER
    // =========================================================================
    public EmailMessage renderWelcomeRecruiter(User user, String frontendBaseUrl) {
        String name = escape(formatName(user));
        String postJobUrl = sanitizeUrl(frontendBaseUrl) + "/recruiter/jobs/new";
        String subject = "Welcome to HireAI - Scale Your Technical Hiring";

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #112358, #004499); padding: 30px; text-align: center; color: #ffffff; }
                .content { padding: 32px 28px; }
                .btn { display: inline-block; padding: 12px 24px; background: #004499; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 14px; margin-top: 15px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1 style="margin:0; font-size:24px;">Welcome to HireAI</h1>
                  <p style="margin:8px 0 0; font-size:14px; color:#94a3b8;">High-Precision Recruitment Pipeline</p>
                </div>
                <div class="content">
                  <p style="font-size:15px;">Welcome <strong>%s</strong>,</p>
                  <p style="font-size:14px; color:#475569; line-height:1.6;">
                    Thank you for partnering with HireAI. Publish your openings and tap into automated ATS scoring and multi-skill matrices.
                  </p>
                  <p style="text-align: center; margin-top:25px;">
                    <a href="%s" class="btn" style="color:#ffffff;">Create First Job Posting</a>
                  </p>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Platform. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """, name, postJobUrl);

        String plainText = String.format("""
            Welcome to HireAI, %s!
            Publish your first job posting at: %s

            (c) 2026 HireAI Platform.
            """, name, postJobUrl);

        return EmailMessage.builder()
                .to(user.getEmail())
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("WELCOME_RECRUITER")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    // =========================================================================
    // 14. ADMIN: SUB-ADMIN CREATED
    // =========================================================================
    public EmailMessage renderSubAdminCreated(String email, String adminEmail, String frontendBaseUrl) {
        String loginUrl = sanitizeUrl(frontendBaseUrl) + "/login";
        String subject = "HireAI Sub-Administrator Account Created";

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #112358, #004499); padding: 30px; text-align: center; color: #ffffff; }
                .content { padding: 32px 28px; }
                .btn { display: inline-block; padding: 12px 24px; background: #004499; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 14px; margin-top: 15px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h2 style="margin:0; font-size:24px;">Administrative Account Created</h2>
                </div>
                <div class="content">
                  <p style="font-size:15px;">Hello,</p>
                  <p style="font-size:14px; color:#475569; line-height:1.6;">
                    A sub-administrator account has been provisioned for your email address on the HireAI platform by <strong>%s</strong>.
                  </p>
                  <p style="font-size:13px; color:#64748b; line-height:1.6;">
                    You will be required to update your temporary password upon your first sign-in.
                  </p>
                  <p style="text-align: center; margin-top:25px;">
                    <a href="%s" class="btn" style="color:#ffffff;">Sign In to Admin Portal</a>
                  </p>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Platform. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """, escape(adminEmail), loginUrl);

        String plainText = String.format("""
            Hello,

            A sub-administrator account was created for your email on HireAI by %s.
            Sign in at: %s

            (c) 2026 HireAI Platform.
            """, adminEmail, loginUrl);

        return EmailMessage.builder()
                .to(email)
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("SUB_ADMIN_CREATED")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    // =========================================================================
    // 15. DIAGNOSTICS: TEST EMAIL
    // =========================================================================
    public EmailMessage renderTestEmail(String recipientEmail, String frontendBaseUrl) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String subject = "HireAI Email Delivery Diagnostics - Test Message";

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #112358, #004499); padding: 30px; text-align: center; color: #ffffff; }
                .content { padding: 32px 28px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h2 style="margin:0; font-size:24px;">Diagnostics Verification</h2>
                  <p style="margin:6px 0 0; font-size:14px; opacity:0.9;">Outbound Transport Validation</p>
                </div>
                <div class="content">
                  <p style="font-size:15px; color:#10b981; font-weight:700;">Outbound Transport Test Successful</p>
                  <p style="font-size:14px; color:#475569; line-height:1.6;">
                    If you are reading this message, the HireAI outbound email notification subsystem is properly connected to its configured transport provider.
                  </p>
                  <p style="font-size:13px; color:#64748b;">Dispatched at: <strong>%s</strong></p>
                </div>
                <div class="footer">
                  (c) 2026 HireAI Platform. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """, timestamp);

        String plainText = String.format("""
            HireAI Email Delivery Diagnostics
            Outbound transport test successful at: %s

            (c) 2026 HireAI Platform.
            """, timestamp);

        return EmailMessage.builder()
                .to(recipientEmail)
                .subject(subject)
                .htmlContent(html)
                .plainTextContent(plainText)
                .emailType("TEST_EMAIL")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    // =========================================================================
    // 16. GENERIC HTML EMAIL
    // =========================================================================
    public EmailMessage renderGenericHtml(String toEmail, String subject, String htmlContent) {
        String plainText = htmlToPlainText(htmlContent);
        return EmailMessage.builder()
                .to(toEmail)
                .subject(subject)
                .htmlContent(htmlContent)
                .plainTextContent(plainText)
                .emailType("GENERIC_HTML")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    // =========================================================================
    // HELPER UTILITIES
    // =========================================================================

    private String formatName(User user) {
        if (user == null) {
            return "User";
        }
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return !full.isEmpty() ? full : "User";
    }

    private String escape(String input) {
        return input != null ? HtmlUtils.htmlEscape(input) : "";
    }

    private String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5173";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String htmlToPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return html
                .replaceAll("(?i)<style[^>]*>[\\s\\S]*?</style>", "")
                .replaceAll("(?i)<head[^>]*>[\\s\\S]*?</head>", "")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n\n")
                .replaceAll("(?i)</div>", "\n")
                .replaceAll("(?i)</h1>|</h2>|</h3>", "\n\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
