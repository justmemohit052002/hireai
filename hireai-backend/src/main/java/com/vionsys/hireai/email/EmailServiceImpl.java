package com.vionsys.hireai.email;

import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.vionsys.hireai.application.dto.JobApplicationResponse;
import com.vionsys.hireai.application.enums.ApplicationStatus;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailProperties emailProperties;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // =========================================================================
    // 1. CANDIDATE: APPLICATION SUBMITTED CONFIRMATION
    // =========================================================================

    @Async
    @Override
    public void sendApplicationReceivedToCandidate(JobApplicationResponse application) {
        if (!emailProperties.isEnabled() || application == null || application.getCandidateEmail() == null) {
            return;
        }

        String candidateEmail = application.getCandidateEmail();
        String candidateName = application.getCandidateName() != null ? application.getCandidateName() : "Candidate";
        String jobTitle = application.getJobTitle() != null ? application.getJobTitle() : "Job Position";
        String companyName = application.getCompanyName() != null ? application.getCompanyName() : "Vionsys Technologies";

        int score = application.getAtsMatchScore() != null ? application.getAtsMatchScore().intValue() : 0;
        ApplicationStatus status = application.getStatus();

        String statusBadgeColor = status == ApplicationStatus.SHORTLISTED ? "#10b981" : (status == ApplicationStatus.REJECTED ? "#ef4444" : "#3b82f6");
        String statusText = status == ApplicationStatus.SHORTLISTED
                ? "SHORTLISTED FOR INTERVIEW"
                : (status == ApplicationStatus.REJECTED ? "NOT SELECTED" : "UNDER REVIEW");

        String subject = String.format("Application Received: %s at %s", jobTitle, companyName);

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f7fa; margin: 0; padding: 20px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                .header { background: linear-gradient(135deg, #1e293b, #0f172a); padding: 30px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px; }
                .header p { margin: 8px 0 0 0; color: #94a3b8; font-size: 14px; }
                .content { padding: 30px; }
                .greeting { font-size: 16px; margin-bottom: 20px; line-height: 1.5; }
                .card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; margin: 20px 0; }
                .card-item { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 14px; }
                .card-item:last-child { margin-bottom: 0; }
                .label { color: #64748b; font-weight: 500; }
                .value { font-weight: 600; color: #0f172a; }
                .badge { display: inline-block; padding: 6px 14px; border-radius: 20px; font-size: 13px; font-weight: 700; color: #ffffff; background: %s; text-align: center; }
                .score-box { text-align: center; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 15px; margin: 20px 0; }
                .score-num { font-size: 28px; font-weight: 800; color: #16a34a; }
                .notes { background: #fffbeb; border-left: 4px solid #f59e0b; padding: 12px 16px; border-radius: 4px; font-size: 13px; color: #92400e; margin: 15px 0; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>HireAI</h1>
                  <p>Intelligent Recruitment & Talent Acceleration</p>
                </div>
                <div class="content">
                  <div class="greeting">
                    Dear <strong>%s</strong>,<br><br>
                    Thank you for applying for the position of <strong>%s</strong> at <strong>%s</strong>. We have received your application and processed your profile through our AI ATS engine.
                  </div>

                  <div class="card">
                    <div class="card-item">
                      <span class="label">Job Title:</span>
                      <span class="value">%s</span>
                    </div>
                    <div class="card-item">
                      <span class="label">Company:</span>
                      <span class="value">%s</span>
                    </div>
                    <div class="card-item">
                      <span class="label">Current Status:</span>
                      <span class="badge">%s</span>
                    </div>
                    <div class="card-item">
                      <span class="label">ATS Match Score:</span>
                      <span class="value">%d%%</span>
                    </div>
                  </div>

                  %s

                  <p style="font-size: 14px; color: #475569; line-height: 1.6;">
                    Our recruiting team will review your application. You can track the progress of your application live in your HireAI candidate dashboard.
                  </p>
                </div>
                <div class="footer">
                  © 2026 HireAI Platform. All rights reserved.<br>
                  This is an automated notification. Please do not reply directly to this email.
                </div>
              </div>
            </body>
            </html>
            """,
                statusBadgeColor,
                candidateName,
                jobTitle,
                companyName,
                jobTitle,
                companyName,
                statusText,
                score,
                application.getRecruiterNotes() != null
                        ? String.format("<div class=\"notes\"><strong>Status Note:</strong> %s</div>", application.getRecruiterNotes())
                        : ""
        );

        sendHtmlEmail(candidateEmail, subject, htmlContent);
    }

    // =========================================================
    // 2. RECRUITER: NEW APPLICANT ALERT
    // =========================================================

    @Async
    @Override
    public void sendNewApplicantAlertToRecruiter(JobApplicationResponse application, String recruiterEmail) {
        if (!emailProperties.isEnabled() || application == null || recruiterEmail == null || recruiterEmail.isBlank()) {
            return;
        }

        String candidateName = application.getCandidateName() != null ? application.getCandidateName() : "Candidate";
        String candidateEmail = application.getCandidateEmail() != null ? application.getCandidateEmail() : "N/A";
        String candidatePhone = application.getCandidatePhone() != null ? application.getCandidatePhone() : "N/A";
        String jobTitle = application.getJobTitle() != null ? application.getJobTitle() : "Job Position";
        int score = application.getAtsMatchScore() != null ? application.getAtsMatchScore().intValue() : 0;
        String status = application.getStatus() != null ? application.getStatus().name() : "APPLIED";

        String matchingSkills = application.getMatchingSkills() != null && !application.getMatchingSkills().isEmpty()
                ? String.join(", ", application.getMatchingSkills())
                : "None";
        String missingSkills = application.getMissingSkills() != null && !application.getMissingSkills().isEmpty()
                ? String.join(", ", application.getMissingSkills())
                : "None";

        String subject = String.format("New Applicant Alert: %s applied for %s (ATS Score: %d%%)", candidateName, jobTitle, score);

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f7fa; margin: 0; padding: 20px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                .header { background: linear-gradient(135deg, #2563eb, #1d4ed8); padding: 30px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 22px; font-weight: 700; }
                .header p { margin: 6px 0 0 0; color: #bfdbfe; font-size: 14px; }
                .content { padding: 30px; }
                .card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; margin: 20px 0; }
                .card-item { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 14px; }
                .label { color: #64748b; font-weight: 500; }
                .value { font-weight: 600; color: #0f172a; }
                .score-badge { display: inline-block; padding: 8px 16px; border-radius: 20px; font-size: 14px; font-weight: 800; color: #ffffff; background: %s; }
                .skills-box { background: #f1f5f9; padding: 15px; border-radius: 8px; margin: 15px 0; font-size: 13px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>HireAI Recruiter Alert</h1>
                  <p>A new candidate has applied to your active job posting</p>
                </div>
                <div class="content">
                  <p style="font-size: 15px; margin-bottom: 20px;">
                    Hello Recruiter,<br><br>
                    A new candidate has submitted an application for <strong>%s</strong>.
                  </p>

                  <div class="card">
                    <div class="card-item"><span class="label">Candidate:</span><span class="value">%s</span></div>
                    <div class="card-item"><span class="label">Email:</span><span class="value">%s</span></div>
                    <div class="card-item"><span class="label">Phone:</span><span class="value">%s</span></div>
                    <div class="card-item"><span class="label">Initial Stage:</span><span class="value">%s</span></div>
                    <div class="card-item"><span class="label">ATS Match Score:</span><span class="score-badge">%d%%</span></div>
                  </div>

                  <div class="skills-box">
                    <p style="margin: 0 0 6px 0;"><strong>Matching Skills:</strong> %s</p>
                    <p style="margin: 0;"><strong>Missing Skills:</strong> %s</p>
                  </div>

                  <p style="font-size: 14px; color: #475569;">
                    You can view the full applicant profile, download their resume, and schedule interviews directly in your recruiter dashboard.
                  </p>
                </div>
                <div class="footer">
                  HireAI Platform • Vionsys Technologies
                </div>
              </div>
            </body>
            </html>
            """,
                score >= 70 ? "#16a34a" : (score < 40 ? "#dc2626" : "#f59e0b"),
                jobTitle,
                candidateName,
                candidateEmail,
                candidatePhone,
                status,
                score,
                matchingSkills,
                missingSkills
        );

        sendHtmlEmail(recruiterEmail, subject, htmlContent);
    }

    // =========================================================
    // 3. CANDIDATE: STATUS UPDATE NOTIFICATION
    // =========================================================

    @Async
    @Override
    public void sendStatusUpdateToCandidate(JobApplicationResponse application, ApplicationStatus newStatus, String recruiterNotes) {
        if (!emailProperties.isEnabled() || application == null || application.getCandidateEmail() == null) {
            return;
        }

        String candidateEmail = application.getCandidateEmail();
        String candidateName = application.getCandidateName() != null ? application.getCandidateName() : "Candidate";
        String jobTitle = application.getJobTitle() != null ? application.getJobTitle() : "Job Position";
        String companyName = application.getCompanyName() != null ? application.getCompanyName() : "Vionsys Technologies";

        String headline;
        String statusDescription;
        String badgeColor;

        switch (newStatus) {
            case INTERVIEW_SCHEDULED -> {
                headline = "Interview Scheduled!";
                statusDescription = "Congratulations! Your application has progressed to the interview stage.";
                badgeColor = "#0284c7";
            }
            case SHORTLISTED -> {
                headline = "You Have Been Shortlisted!";
                statusDescription = "Great news! Your profile meets our key technical criteria and has been shortlisted.";
                badgeColor = "#16a34a";
            }
            case OFFERED -> {
                headline = "Congratulations on Your Job Offer!";
                statusDescription = "We are thrilled to extend an offer of employment for this position.";
                badgeColor = "#8b5cf6";
            }
            case REJECTED -> {
                headline = "Update Regarding Your Application";
                statusDescription = "Thank you for your interest. After review, we will not be moving forward at this time.";
                badgeColor = "#ef4444";
            }
            default -> {
                headline = "Application Status Updated";
                statusDescription = "Your application status has been updated to: " + newStatus.name();
                badgeColor = "#64748b";
            }
        }

        String subject = String.format("%s - %s at %s", headline, jobTitle, companyName);

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f7fa; margin: 0; padding: 20px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                .header { background: %s; padding: 30px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 22px; font-weight: 700; }
                .content { padding: 30px; }
                .card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; margin: 20px 0; }
                .card-item { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 14px; }
                .label { color: #64748b; font-weight: 500; }
                .value { font-weight: 600; color: #0f172a; }
                .notes { background: #f0f9ff; border-left: 4px solid #0284c7; padding: 14px; border-radius: 4px; font-size: 13px; color: #0369a1; margin: 20px 0; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>%s</h1>
                </div>
                <div class="content">
                  <p style="font-size: 15px;">
                    Dear <strong>%s</strong>,<br><br>
                    %s
                  </p>

                  <div class="card">
                    <div class="card-item"><span class="label">Position:</span><span class="value">%s</span></div>
                    <div class="card-item"><span class="label">Company:</span><span class="value">%s</span></div>
                    <div class="card-item"><span class="label">New Status:</span><span class="value" style="color:%s; font-weight:bold;">%s</span></div>
                  </div>

                  %s

                  <p style="font-size: 14px; color: #64748b;">
                    Best regards,<br>
                    <strong>%s Recruiting Team</strong>
                  </p>
                </div>
                <div class="footer">
                  HireAI Platform • Vionsys Technologies
                </div>
              </div>
            </body>
            </html>
            """,
                badgeColor,
                headline,
                candidateName,
                statusDescription,
                jobTitle,
                companyName,
                badgeColor,
                newStatus.name(),
                recruiterNotes != null && !recruiterNotes.isBlank()
                        ? String.format("<div class=\"notes\"><strong>Recruiter Note / Feedback:</strong><br>%s</div>", recruiterNotes)
                        : "",
                companyName
        );

        sendHtmlEmail(candidateEmail, subject, htmlContent);
    }

    // =========================================================
    // 4. CANDIDATE: WELCOME REGISTRATION EMAIL
    // =========================================================

    @Async
    @Override
    public void sendWelcomeCandidateEmail(com.vionsys.hireai.user.entity.User user) {
        if (!emailProperties.isEnabled() || user == null || user.getEmail() == null) {
            return;
        }

        String name = user.getFirstName() + " " + user.getLastName();
        String subject = "Welcome to HireAI! Start Your AI-Powered Career Journey";

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f7fa; margin: 0; padding: 20px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                .header { background: linear-gradient(135deg, #0ea5e9, #0284c7); padding: 30px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 24px; font-weight: 700; }
                .content { padding: 30px; line-height: 1.6; }
                .step-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px; margin: 12px 0; }
                .step-num { display: inline-block; width: 24px; height: 24px; background: #0284c7; color: white; border-radius: 50%%; text-align: center; line-height: 24px; font-weight: bold; margin-right: 8px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Welcome to HireAI</h1>
                  <p style="margin: 6px 0 0 0; color: #e0f2fe;">Your AI-Powered Recruitment & Matching Platform</p>
                </div>
                <div class="content">
                  <p style="font-size: 16px;">Dear <strong>%s</strong>,</p>
                  <p>Welcome aboard! Your candidate account has been successfully created. You can now discover top tech roles and get instant AI ATS match scoring on your applications.</p>

                  <h3 style="color: #0f172a; margin-top: 25px;">Next Steps to Accelerate Your Job Search:</h3>

                  <div class="step-box">
                    <span class="step-num">1</span> <strong>Complete Your Candidate Profile</strong><br>
                    Add your experience, target CTC, notice period, and core skill stack.
                  </div>
                  <div class="step-box">
                    <span class="step-num">2</span> <strong>Upload Your Resume (PDF/DOCX)</strong><br>
                    Our AI parser automatically extracts skills and enriches your profile.
                  </div>
                  <div class="step-box">
                    <span class="step-num">3</span> <strong>Browse & Apply to Open Jobs</strong><br>
                    Get instant ATS match analytics and live application pipeline tracking.
                  </div>

                  <p style="margin-top: 25px; color: #475569;">Best of luck with your job search!<br><strong>The HireAI Team</strong></p>
                </div>
                <div class="footer">
                  © 2026 HireAI Platform • Vionsys Technologies
                </div>
              </div>
            </body>
            </html>
            """, name);

        sendHtmlEmail(user.getEmail(), subject, htmlContent);
    }

    // =========================================================
    // 5. RECRUITER: WELCOME REGISTRATION EMAIL
    // =========================================================

    @Async
    @Override
    public void sendWelcomeRecruiterEmail(com.vionsys.hireai.user.entity.User user) {
        if (!emailProperties.isEnabled() || user == null || user.getEmail() == null) {
            return;
        }

        String name = user.getFirstName() + " " + user.getLastName();
        String subject = "Welcome to HireAI for Employers! Scale Your Talent Acquisition";

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f7fa; margin: 0; padding: 20px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                .header { background: linear-gradient(135deg, #4f46e5, #4338ca); padding: 30px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 24px; font-weight: 700; }
                .content { padding: 30px; line-height: 1.6; }
                .card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px; margin: 12px 0; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>HireAI for Employers</h1>
                  <p style="margin: 6px 0 0 0; color: #e0e7ff;">AI-Driven Applicant Tracking & Candidate Ranking</p>
                </div>
                <div class="content">
                  <p style="font-size: 16px;">Dear <strong>%s</strong>,</p>
                  <p>Welcome to HireAI! Your recruiter account is now active. You have access to intelligent ATS applicant screening, semantic skill matching, and candidate directory management.</p>

                  <h3 style="color: #0f172a; margin-top: 25px;">Get Started:</h3>
                  <div class="card"><strong>1. Set Up Employer Profile:</strong> Add company branding, website, and industry details.</div>
                  <div class="card"><strong>2. Post Vacancies:</strong> Publish job openings with required skills and experience levels.</div>
                  <div class="card"><strong>3. Auto-Rank Applicants:</strong> Review applicants ranked automatically by ATS Match Score.</div>

                  <p style="margin-top: 25px; color: #475569;">Happy Hiring!<br><strong>The HireAI Team</strong></p>
                </div>
                <div class="footer">
                  © 2026 HireAI Platform • Vionsys Technologies
                </div>
              </div>
            </body>
            </html>
            """, name);

        sendHtmlEmail(user.getEmail(), subject, htmlContent);
    }

    // =========================================================
    // 6. USER: LOGIN SECURITY ALERT
    // =========================================================

    @Async
    @Override
    public void sendLoginAlertEmail(com.vionsys.hireai.user.entity.User user) {
        if (!emailProperties.isEnabled() || user == null || user.getEmail() == null) {
            return;
        }

        String name = user.getFirstName() + " " + user.getLastName();
        String timeStr = java.time.LocalDateTime.now().format(DATE_FORMATTER);
        String subject = "New Sign-in to your HireAI account";

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f7fa; margin: 0; padding: 20px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                .header { background: #1e293b; padding: 25px; text-align: center; color: #ffffff; }
                .content { padding: 30px; }
                .alert-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 18px; margin: 20px 0; font-size: 14px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h2 style="margin:0;">HireAI Security Notification</h2>
                </div>
                <div class="content">
                  <p>Hello <strong>%s</strong>,</p>
                  <p>A successful login was recorded for your HireAI account.</p>

                  <div class="alert-box">
                    <p style="margin:0 0 8px 0;"><strong>Account Email:</strong> %s</p>
                    <p style="margin:0 0 8px 0;"><strong>Login Timestamp:</strong> %s</p>
                    <p style="margin:0;"><strong>Authentication Method:</strong> JWT Bearer Password Auth</p>
                  </div>

                  <p style="font-size: 13px; color: #ef4444; margin-top: 20px;">
                    <strong>Security Notice:</strong> If you did not initiate this login, please change your password immediately or contact our support team.
                  </p>
                </div>
                <div class="footer">
                  © 2026 HireAI Platform • Security Operations
                </div>
              </div>
            </body>
            </html>
            """, name, user.getEmail(), timeStr);

        sendHtmlEmail(user.getEmail(), subject, htmlContent);
    }

    // =========================================================
    // 7. USER: PASSWORD RESET TOKEN EMAIL
    // =========================================================

    @Async
    @Override
    public void sendPasswordResetEmail(com.vionsys.hireai.user.entity.User user, String token, java.time.LocalDateTime expiryDate) {
        if (!emailProperties.isEnabled() || user == null || user.getEmail() == null) {
            return;
        }

        String name = user.getFirstName() + " " + user.getLastName();
        String expiryStr = expiryDate != null ? expiryDate.format(DATE_FORMATTER) : "in 15 minutes";
        String subject = "Reset Your HireAI Account Password";

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f7fa; margin: 0; padding: 20px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                .header { background: #dc2626; padding: 25px; text-align: center; color: #ffffff; }
                .content { padding: 30px; }
                .token-box { background: #fef2f2; border: 2px dashed #f87171; border-radius: 8px; padding: 18px; text-align: center; margin: 25px 0; }
                .token-code { font-family: monospace; font-size: 16px; font-weight: bold; color: #991b1b; word-break: break-all; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h2 style="margin:0;">Password Reset Request</h2>
                </div>
                <div class="content">
                  <p>Hello <strong>%s</strong>,</p>
                  <p>We received a request to reset the password for your HireAI account. Use the secure token below to complete your password reset:</p>

                  <div class="token-box">
                    <p style="margin:0 0 10px 0; font-size: 13px; color: #7f1d1d;"><strong>Your Secure Password Reset Token:</strong></p>
                    <div class="token-code">%s</div>
                  </div>

                  <p style="font-size: 13px; color: #64748b;">
                    ⏰ <strong>Note:</strong> This token will expire on <strong>%s</strong>.
                  </p>
                  <p style="font-size: 13px; color: #94a3b8;">
                    If you did not request a password reset, you can safely ignore this email. Your password will remain unchanged.
                  </p>
                </div>
                <div class="footer">
                  © 2026 HireAI Platform • Security Team
                </div>
              </div>
            </body>
            </html>
            """, name, token, expiryStr);

        sendHtmlEmail(user.getEmail(), subject, htmlContent);
    }

    // =========================================================
    // 4. CORE ASYNCHRONOUS HTML EMAIL SENDER
    // =========================================================

    @Async
    @Override
    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        if (!emailProperties.isEnabled()) {
            log.info("Email service disabled via properties. Skipping email to {}", toEmail);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.info("[MOCK EMAIL DISPATCH] (No SMTP configured - simulation mode)\n"
                            + "==================================================\n"
                            + "TO: {}\n"
                            + "FROM: {} <{}>\n"
                            + "SUBJECT: {}\n"
                            + "==================================================",
                    toEmail, emailProperties.getSenderName(), emailProperties.getFrom(), subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailProperties.getFrom(), emailProperties.getSenderName());
            helper.setTo(toEmail);
            helper.setSubject(subject);

            // Generate a clean plain-text fallback for multipart/alternative (significantly reduces spam score)
            String plainText = htmlToPlainText(htmlContent);
            helper.setText(plainText, htmlContent);

            // Add standard transactional headers to inform spam filters this is an automated system email
            message.setHeader("Auto-Submitted", "auto-generated");
            message.setHeader("X-Auto-Response-Suppress", "All");
            message.setHeader("X-Mailer", "HireAI-Notification-Service/1.0");

            mailSender.send(message);
            log.info("Email successfully dispatched to {}", toEmail);

        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send email to {}: {}. Logging notification locally.", toEmail, ex.getMessage());
            log.info("[FALLBACK EMAIL LOG]\nTO: {}\nSUBJECT: {}\nCONTENT SNIPPET: {}",
                    toEmail, subject, htmlContent.substring(0, Math.min(200, htmlContent.length())));
        } catch (Exception ex) {
            log.warn("SMTP host unreachable ({}). Simulated email dispatch for {}", ex.getMessage(), toEmail);
        }
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
