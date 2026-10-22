# HireAI Email Notification System - Forensics Investigation Report

**Investigation Date:** October 19, 2026  
**Repository Branch:** `feature/mohit`  
**Target Subsystem:** `hireai-backend` (Spring Boot 3.5.16 / Java 21)

---

## 1. Executive Summary

The email notification system in HireAI was reported as completely non-functional across all user flows (registration verification OTPs, sign-in alerts, password resets, application confirmations, and recruiter alerts). Backend startup logs revealed:
```
No active profile set, falling back to default
...
[SIMULATED EMAIL DISPATCH] (Dev Profile Active - SMTP Unconfigured) ...
```

A deep Git forensic analysis across code, commit history, and configuration files identified the **exact root cause** and timeline of events:
1. **Working State (Commit `a42d988` - Sep 6, 2026):**
   A valid Gmail SMTP App Password was hardcoded as the default fallback in `application.properties`:
   `spring.mail.password=${SPRING_MAIL_PASSWORD:<app_password>}`. Real delivery worked out-of-the-box in local and dev environments.
2. **Breakage Trigger (Commit `d423b06` - Oct 13, 2026):**
   `spring.mail.password` was sanitized to `${SPRING_MAIL_PASSWORD:}` (empty fallback) to avoid committing secrets. However, no `.env` file loader was configured in Spring Boot, and `SPRING_MAIL_PASSWORD` was not set in the runtime environment.
3. **Permanent Lockout in Simulation Mode (Commit `6ef11c8` - Oct 14, 2026):**
   Commit `6ef11c8` introduced `isSmtpConfigured()` in `EmailServiceImpl.java`:
   ```java
   public boolean isSmtpConfigured() {
       ...
       if (mailSender instanceof JavaMailSenderImpl impl) {
           return impl.getPassword() != null && !impl.getPassword().isBlank();
       }
       return true;
   }
   ```
   Because `SPRING_MAIL_PASSWORD` was unset, `impl.getPassword()` resolved to `""` (empty string). As a result, `isSmtpConfigured()` evaluated to `false`. Every email method immediately routed into simulation logs and returned without sending.
4. **Log Profile Misattribution:**
   The code checked `environment.acceptsProfiles(Profiles.of("dev", "test", "default"))`. When Spring Boot runs without an active profile, it defaults to `"default"`, which matched this condition and logged `[SIMULATED EMAIL DISPATCH] (Dev Profile Active - SMTP Unconfigured)` even though the `dev` profile was not active.

---

## 2. Configuration Audit

| Property Name | Source File | Configured Expression | Resolved Value | Evaluation |
|---|---|---|---|---|
| `spring.mail.host` | `application.properties` | `${SPRING_MAIL_HOST:smtp.gmail.com}` | `smtp.gmail.com` | Non-empty |
| `spring.mail.port` | `application.properties` | `${SPRING_MAIL_PORT:587}` | `587` | Non-empty (STARTTLS) |
| `spring.mail.username` | `application.properties` | `${SPRING_MAIL_USERNAME:mohitpc2002@gmail.com}` | `mohitpc2002@gmail.com` | Non-empty |
| `spring.mail.password` | `application.properties` | `${SPRING_MAIL_PASSWORD:}` | `""` (Empty String) | **EMPTY** - Causes `isSmtpConfigured()` to fail |
| `spring.mail.properties.mail.smtp.auth` | `application.properties` | `true` | `true` | Non-empty |
| `spring.mail.properties.mail.smtp.starttls.enable` | `application.properties` | `true` | `true` | Non-empty |
| `spring.mail.properties.mail.smtp.connectiontimeout`| `application.properties` | `5000` | `5000` ms | Non-empty |
| `spring.mail.properties.mail.smtp.timeout` | `application.properties` | `5000` | `5000` ms | Non-empty |
| `spring.mail.properties.mail.smtp.writetimeout` | `application.properties` | `5000` | `5000` ms | Non-empty |
| `hireai.mail.enabled` | `application.properties` | `true` | `true` | Non-empty |
| `hireai.mail.from` | `application.properties` | `mohitpc2002@gmail.com` | `mohitpc2002@gmail.com` | Matches username |
| `hireai.mail.sender-name` | `application.properties` | `HireAI Recruitment Platform` | `HireAI Recruitment Platform`| Non-empty |
| Frontend Base URL | N/A | **Not defined** | N/A | Missing - Templates lack links |

---

## 3. Inventory of Email & Notification Triggers

| # | Trigger Event | Caller Method | EmailService Method | Template Used | Recipient | Execution Mode | Current Status | Evidence / Notes |
|---|---|---|---|---|---|---|---|---|
| 1 | Recruiter Registration | `AuthService.registerRecruiter` | `sendEmailVerificationOtp` | In-code HTML OTP card | Recruiter Email | Sync (via post-commit) | SIMULATED | `AuthService:163`; `isSmtpConfigured()=false` |
| 2 | Candidate Registration | `AuthService.registerCandidate` | `sendEmailVerificationOtp` | In-code HTML OTP card | Candidate Email | Sync (via post-commit) | SIMULATED | `AuthService:245`; `isSmtpConfigured()=false` |
| 3 | Resend Verification OTP | `AuthService.resendVerificationOtp` | `sendEmailVerificationOtp` | In-code HTML OTP card | User Email | Sync (via post-commit) | SIMULATED | `AuthService:398`; `isSmtpConfigured()=false` |
| 4 | First-time OTP Verification (Recruiter) | `AuthService.verifyEmailOtp` | `sendWelcomeRecruiterEmail` | In-code HTML welcome | Recruiter Email | Async (`@Async`) | SIMULATED | `AuthService:331`; calls `sendHtmlEmail` |
| 5 | First-time OTP Verification (Candidate) | `AuthService.verifyEmailOtp` | `sendWelcomeCandidateEmail` | In-code HTML welcome | Candidate Email | Async (`@Async`) | SIMULATED | `AuthService:333`; calls `sendHtmlEmail` |
| 6 | Standard Password Login | `AuthService.login` | `sendLoginAlertEmail` | In-code HTML security alert | User Email | Async (`@Async`) | SIMULATED | `AuthService:513`; calls `sendHtmlEmail` |
| 7 | OAuth Login (Google/LinkedIn) | `AuthService.loginWithGoogle/LinkedIn` | None | None | User Email | N/A | **NOT CALLED** | `AuthService:666-680` sends in-app notification but omits email |
| 8 | OAuth Initial Registration | `AuthService.loginWithGoogle/LinkedIn` | `sendWelcomeRecruiterEmail` / `sendWelcomeCandidateEmail` | In-code HTML welcome | User Email | Async (`@Async`) | SIMULATED | `AuthService:657,659`; calls `sendHtmlEmail` |
| 9 | Forgot Password Request | `AuthService.forgotPassword` | `sendPasswordResetEmail` | In-code HTML reset token | User Email | Sync (via post-commit) | SIMULATED | `AuthService:909`; `isSmtpConfigured()=false` |
| 10 | Request Set-Password OTP | `UserServiceImpl.requestSetPasswordOtp` | `sendSetPasswordOtp` | In-code HTML 6-digit OTP | User Email | Sync (in transaction) | SIMULATED | `UserServiceImpl:239`; `isSmtpConfigured()=false` |
| 11 | Change Password Action | `UserServiceImpl.changePassword` | None | None | User Email | N/A | **NOT CALLED** | `UserServiceImpl:187` sends in-app alert, no confirmation email |
| 12 | Request Delete-Account OTP | `UserServiceImpl.requestDeleteAccountOtp` | `sendDeleteAccountOtp` | In-code HTML 6-digit OTP | User Email | Sync (in transaction) | SIMULATED | `UserServiceImpl:468`; `isSmtpConfigured()=false` |
| 13 | Confirm Delete-Account Action | `UserServiceImpl.confirmDeleteAccount` | None | None | User Email | N/A | **NOT CALLED** | `UserServiceImpl:513` soft-deletes user, no confirmation email |
| 14 | Candidate Submits Job Application | `JobApplicationServiceImpl.applyForJob` | `sendApplicationReceivedToCandidate` | In-code HTML + ATS score | Candidate Email | Async (`@Async`) | SIMULATED | `JobApplicationServiceImpl:156`; calls `sendHtmlEmail` |
| 15 | Recruiter Notified of New Applicant | `JobApplicationServiceImpl.applyForJob` | `sendNewApplicantAlertToRecruiter` | In-code HTML + match score | Recruiter Email | Async (`@Async`) | SIMULATED | `JobApplicationServiceImpl:158`; calls `sendHtmlEmail` |
| 16 | Application Status Change | `JobApplicationServiceImpl.updateApplicationStatus` | `sendStatusUpdateToCandidate` | In-code HTML stage badge | Candidate Email | Async (`@Async`) | SIMULATED | `JobApplicationServiceImpl:296`; calls `sendHtmlEmail` |
| 17 | Job Created / Posted | `JobServiceImpl.createJob` | None | None | Recruiter Email | N/A | **NOT CALLED** | Not implemented in `JobServiceImpl` |
| 18 | Job Closed | `JobServiceImpl.closeJob` | None | None | Recruiter Email | N/A | **NOT CALLED** | Not implemented in `JobServiceImpl` |
| 19 | Chat / Direct Message | `ChatServiceImpl.sendMessage` | None | None | Recipient Email | N/A | NOT CALLED (BY DESIGN) | Handled via WebSocket STOMP & in-app notification bell |
| 20 | Sub-Admin Account Creation | `AdminServiceImpl.createSubAdmin` | `sendHtmlEmail` | In-code HTML credentials | Sub-Admin Email | Sync | SIMULATED | `AdminServiceImpl:476`; checks `isSmtpConfigured()` |
| 21 | Admin Test Email | `AdminServiceImpl.sendTestEmail` | `sendHtmlEmail` | In-code HTML diagnostic | Recipient Email | Sync | SIMULATED | `AdminServiceImpl:773`; calls `sendHtmlEmail` |

---

## 4. Runtime & Architecture Findings

1. **Silent Simulation in Non-Dev Profiles:**
   When running under `default` or `prod` without SMTP credentials, the service logs `[SIMULATED EMAIL DISPATCH]` and returns normally. Endpoints return `200 OK`, leaving users believing an email was dispatched when it was discarded.
2. **Missing Dedicated Async Executor:**
   `@EnableAsync` is active without a dedicated `ThreadPoolTaskExecutor` bean for email. Async tasks run on Spring Boot's shared general-purpose executor with unbounded queueing risks.
3. **Self-Invocation Proxy Bypass:**
   In `EmailServiceImpl`, `@Async` methods like `sendApplicationReceivedToCandidate` invoke `this.sendHtmlEmail(...)` internally, bypassing proxy mechanics for the inner transport call.
4. **HTML Injection Vulnerability in Templates:**
   Templates perform raw string formatting (`%s`) with user-supplied values (`candidateName`, `jobTitle`, `recruiterNotes`) without HTML escaping, opening risks of layout distortion and HTML injection in recipient mail clients.
5. **Missing Frontend Base URL:**
   Email templates contain no clickable links or actionable buttons directing users to their dashboard.
6. **Provider-Side (Gmail) Prerequisites:**
   Gmail requires an App Password with 2-Step Verification enabled. Standard passwords trigger `535-5.7.8 Username and Password not accepted`.

---

## 5. Next Steps for Phase 2 Implementation

1. **Introduce Explicit Mode Selection:**
   Configure `hireai.mail.mode=SMTP|LOG`. In `SMTP` mode with missing credentials, log prominent errors or fail fast based on profile. In `LOG` mode, output clear startup warnings.
2. **Implement Dedicated Bounded Async Executor:**
   Define `emailTaskExecutor` (e.g. pool size 4-8, queue capacity 500, `CallerRunsPolicy` or logging rejection) isolated from WebSocket/scheduling pools.
3. **Template Renderer & HTML Escaping:**
   Separate HTML rendering from transport, sanitize dynamic variables, inject configurable `frontendBaseUrl`, and add plain-text fallbacks.
4. **Wire Missing Triggers:**
   Add email dispatch for OAuth login alert, password change confirmation, account deletion confirmation, and recruiter job posting confirmation.
5. **Integration & Unit Testing with GreenMail:**
   Add test-scoped GreenMail to `pom.xml` to test real SMTP transport on ephemeral ports during test runs.
