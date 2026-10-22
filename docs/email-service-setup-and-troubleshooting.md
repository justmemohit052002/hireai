# HireAI Email Notification Subsystem Guide

This document describes the architecture, configuration, testing procedures, and troubleshooting guidelines for the HireAI outbound email notification subsystem.

---

## 1. Architecture Overview

The HireAI email subsystem follows a clean Hexagonal / Ports-and-Adapters architectural pattern:

```
[Business Services] (AuthService, JobApplicationService, JobService, UserService, AdminService)
          |
          v
  [EmailService]  (Public API with intention-revealing methods)
          |
          +---> [EmailTemplateRenderer] (Escapes HTML, generates plain-text, applies official blues #004499)
          |
          +---> [TransactionSynchronizationManager] (Safe AFTER_COMMIT dispatch; no emails on rollback)
          |
          v
  [EmailDispatcher] (Spring CGLIB Proxy on dedicated 'emailTaskExecutor' thread pool)
          |
     +----+----+
     |         |
     v         v
[SmtpEmailTransport]        [LogEmailTransport]
(Real SMTP with retries)     (Safe console logging for dev/test)
```

### Core Components
- **`EmailService`**: Stable public interface used across the codebase.
- **`EmailTemplateRenderer`**: Dedicated rendering service that escapes dynamic user input (`HtmlUtils.htmlEscape`), prevents injection attacks, generates synchronized plain-text alternatives, and strictly complies with platform visual standards (official brand blues `#004499`, `#0192C6`, `#112358`, neutral slates, `rounded-lg` borders, no purple, no emojis, no em dashes).
- **`EmailTransport`**: Port contract decoupling transport technology from application logic.
- **`SmtpEmailTransport`**: Production transport supporting exponential backoff for transient network issues (socket timeout, connection reset), permanent failure fail-fast (authentication failures, invalid addresses), and CRLF header injection sanitization.
- **`LogEmailTransport`**: Console simulation transport for development and testing. Outside `dev` and `test` Spring profiles, verification OTPs and security tokens are masked to prevent accidental log exposure.
- **`EmailDispatcher`**: Bounded, dedicated thread pool (`hireai-mail-`, pool size 2-8, queue 500, `CallerRunsPolicy`) completely isolated from the WebSocket broker scheduler.
- **Transactional Safety**: Outbound messages are registered via Spring's `TransactionSynchronizationManager` to fire only upon successful database commit (`afterCommit`). If a business transaction rolls back, zero emails are sent.

---

## 2. Configuration & Environment Variables

Outbound delivery is controlled via typed properties prefixed with `hireai.mail.*` and Spring Boot's standard `spring.mail.*`.

### Required Environment Variable Names (Never commit actual values)

| Variable Name | Description | Default Value | Recommended Setting |
| :--- | :--- | :--- | :--- |
| `HIREAI_MAIL_MODE` | Outbound delivery mode (`SMTP` or `LOG`) | `SMTP` | `SMTP` for production; `LOG` for offline dev |
| `HIREAI_MAIL_ENABLED` | Master email switch (`true` / `false`) | `true` | `true` |
| `HIREAI_MAIL_FROM` | RFC-compliant sender email address | `notifications@vionsys.com` | Verified authenticated domain/account |
| `HIREAI_MAIL_SENDER_NAME` | Sender display name in inboxes | `HireAI Recruitment Platform` | `HireAI Recruitment Platform` |
| `HIREAI_FRONTEND_URL` | Base URL used to build CTA buttons | `http://localhost:5173` | Production domain (e.g. `https://hireai.com`) |
| `SPRING_MAIL_HOST` | SMTP server hostname | `smtp.gmail.com` | `smtp.gmail.com` or custom relay |
| `SPRING_MAIL_PORT` | SMTP server port | `587` | `587` (STARTTLS) or `465` (SSL) |
| `SPRING_MAIL_USERNAME` | SMTP account username / email | Unset / empty | Authenticated account |
| `SPRING_MAIL_PASSWORD` | SMTP password or App Password | Unset / empty | 16-character App Password (no spaces) |
| `SPRING_MAIL_CONNECTION_TIMEOUT` | Socket connection timeout in ms | `10000` | `10000` |
| `SPRING_MAIL_READ_TIMEOUT` | Socket read timeout in ms | `15000` | `15000` |
| `SPRING_MAIL_WRITE_TIMEOUT` | Socket write timeout in ms | `15000` | `15000` |

---

## 3. Provider Setup: Gmail SMTP & App Passwords

When sending via Gmail SMTP (`smtp.gmail.com:587`), standard Google account passwords will **not** work and will return `535 5.7.8 Username and Password not accepted`. You must generate a dedicated **Google App Password**:

1. Log in to your Google Account and navigate to **Security** (`https://myaccount.google.com/security`).
2. Under "How you sign in to Google", ensure **2-Step Verification** is turned **ON**. (App Passwords cannot be created without 2-Step Verification).
3. Search for or navigate to **App passwords** (`https://myaccount.google.com/apppasswords`).
4. Enter an app name (e.g., `HireAI Backend Production`) and click **Create**.
5. Google will generate a 16-character code (formatted as `xxxx xxxx xxxx xxxx`).
6. Remove all spaces and set as your environment variable:
   ```bash
   export SPRING_MAIL_PASSWORD=xxxxxxxxxxxxxxxx
   ```
7. Ensure `SPRING_MAIL_USERNAME` matches the exact email address of that Google account, and `HIREAI_MAIL_FROM` matches this authenticated address to avoid Gmail rejection or DMARC/SPF penalties.

---

## 4. Local Testing with Mailpit / MailHog / GreenMail

For local testing without sending real emails:

### Option A: Using Built-in LOG Mode (Zero Setup Required)
Set in your local environment:
```powershell
$env:HIREAI_MAIL_MODE = "LOG"
```
All emails will be simulated and printed cleanly to the console log with masked recipient addresses and security notices.

### Option B: Local SMTP Server (Mailpit / MailHog)
1. Run Mailpit via Docker:
   ```bash
   docker run -d -p 1025:1025 -p 8025:8025 axllent/mailpit
   ```
2. Set backend environment variables:
   ```properties
   HIREAI_MAIL_MODE=SMTP
   SPRING_MAIL_HOST=localhost
   SPRING_MAIL_PORT=1025
   SPRING_MAIL_USERNAME=test@hireai.local
   SPRING_MAIL_PASSWORD=test
   ```
3. Open `http://localhost:8025` in your browser to inspect all outbound emails in real time.

---

## 5. Troubleshooting Matrix

| Symptom | Probable Cause | Corrective Action |
| :--- | :--- | :--- |
| Log shows `[SIMULATED EMAIL DISPATCH] (Mode: LOG)` | `HIREAI_MAIL_MODE` is explicitly set to `LOG` or `isSmtpConfigured()` detected blank credentials. | Set `HIREAI_MAIL_MODE=SMTP` and supply non-empty `SPRING_MAIL_USERNAME` and `SPRING_MAIL_PASSWORD`. |
| `535 5.7.8 Username and Password not accepted` | Standard password used instead of Gmail App Password, or App Password revoked. | Generate a 16-character App Password under Google 2-Step Verification; remove spaces. |
| Socket connection timed out (`ConnectException` / `TimeoutException`) | Port 587/465 blocked by firewall or network provider, or incorrect host. | Verify outgoing port 587 is accessible (`Test-NetConnection smtp.gmail.com -Port 587`). |
| Emails delivered to Spam / Junk folder | Missing SPF/DKIM records or `HIREAI_MAIL_FROM` does not match authenticated SMTP user. | Align `HIREAI_MAIL_FROM` with `SPRING_MAIL_USERNAME`; configure SPF `include:_spf.google.com ~all`. |
| Action links in email open `http://localhost:5173` in production | `HIREAI_FRONTEND_URL` is unconfigured. | Set `HIREAI_FRONTEND_URL=https://your-production-domain.com`. |
| Registration or job application fails if mail server is down | Critical vs non-critical failure policy. | Verification OTP failures produce clear user error ("Please try Resend"); application submissions are non-critical and will not roll back database transactions. |
