package com.vionsys.hireai.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.vionsys.hireai.application.dto.JobApplicationResponse;
import com.vionsys.hireai.application.enums.ApplicationStatus;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.user.entity.User;

import jakarta.mail.internet.MimeMessage;

/**
 * End-to-end integration test running against an in-memory GreenMail SMTP test server.
 * Verifies that outbound email messages are transmitted across real SMTP sockets,
 * properly formatted as multipart MIME, and receivable by the target mailbox.
 */
class EmailServiceIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("test@hireai.local", "secret"));

    private EmailProperties emailProperties;
    private JavaMailSenderImpl mailSender;
    private EmailServiceImpl emailService;

    private User candidateUser;
    private User recruiterUser;

    @BeforeEach
    void setUp() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
        greenMail.setUser("test@hireai.local", "test@hireai.local", "secret");

        emailProperties = new EmailProperties();
        emailProperties.setEnabled(true);
        emailProperties.setMode(EmailMode.SMTP);
        emailProperties.setFrom("test@hireai.local");
        emailProperties.setSenderName("HireAI Platform");
        emailProperties.setFrontendBaseUrl("http://localhost:5173");
        emailProperties.setMaxRetries(1);
        emailProperties.setRetryDelayMs(50L);

        mailSender = new JavaMailSenderImpl();
        mailSender.setHost("127.0.0.1");
        mailSender.setPort(greenMail.getSmtp().getPort());
        mailSender.setUsername("test@hireai.local");
        mailSender.setPassword("secret");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("mailSender", mailSender);

        StandardEnvironment environment = new StandardEnvironment();
        emailService = new EmailServiceImpl(
                new org.springframework.beans.factory.support.StaticListableBeanFactory() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> org.springframework.beans.factory.ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
                        return new org.springframework.beans.factory.ObjectProvider<T>() {
                            @Override
                            public T getObject() { return (T) mailSender; }
                            @Override
                            public T getObject(Object... args) { return (T) mailSender; }
                            @Override
                            public T getIfAvailable() { return (T) mailSender; }
                            @Override
                            public T getIfUnique() { return (T) mailSender; }
                        };
                    }
                }.getBeanProvider(org.springframework.mail.javamail.JavaMailSender.class),
                emailProperties,
                environment
        );

        candidateUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Candidate")
                .email("john.candidate@hireai.test")
                .role(Role.builder().id(1L).name(RoleType.ROLE_CANDIDATE).build())
                .build();

        recruiterUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Sarah")
                .lastName("Recruiter")
                .email("sarah.recruiter@hireai.test")
                .role(Role.builder().id(2L).name(RoleType.ROLE_RECRUITER).build())
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    @Test
    @DisplayName("End-to-end: Registration OTP email reaches SMTP inbox with valid code")
    void testEndToEnd_RegistrationOtp() throws Exception {
        emailService.sendEmailVerificationOtp(candidateUser, "749201", 10);

        assertTrue(greenMail.waitForIncomingEmail(5000L, 1));
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);

        MimeMessage msg = messages[0];
        assertEquals("john.candidate@hireai.test", msg.getAllRecipients()[0].toString());
        assertEquals("Verify Your Email Address - HireAI", msg.getSubject());
        String body = GreenMailUtil.getBody(msg);
        assertTrue(body.contains("749201"), "Verification code must be present in body");
        assertTrue(body.contains("10 minutes"));
    }

    @Test
    @DisplayName("End-to-end: Login security alert delivers via SMTP with recipient and timestamp")
    void testEndToEnd_LoginAlert() throws Exception {
        emailService.sendLoginAlertEmail(candidateUser);

        assertTrue(greenMail.waitForIncomingEmail(5000L, 1));
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);

        MimeMessage msg = messages[0];
        assertEquals("john.candidate@hireai.test", msg.getAllRecipients()[0].toString());
        assertEquals("Security Alert: New Sign-In to Your HireAI Account", msg.getSubject());
        String body = GreenMailUtil.getBody(msg);
        assertTrue(body.contains("john.candidate@hireai.test"));
        assertTrue(body.contains("http://localhost:5173/settings"));
    }

    @Test
    @DisplayName("End-to-end: Job application confirmation delivers to candidate")
    void testEndToEnd_ApplicationReceived() throws Exception {
        JobApplicationResponse app = JobApplicationResponse.builder()
                .id(UUID.randomUUID())
                .candidateId(candidateUser.getId())
                .candidateName("John Candidate")
                .candidateEmail("john.candidate@hireai.test")
                .jobId(UUID.randomUUID())
                .jobTitle("Principal Cloud Architect")
                .companyName("Nexus Innovations")
                .status(ApplicationStatus.SHORTLISTED)
                .atsMatchScore(88)
                .recruiterNotes("Selected for technical screening.")
                .build();

        emailService.sendApplicationReceivedToCandidate(app);

        assertTrue(greenMail.waitForIncomingEmail(5000L, 1));
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);

        MimeMessage msg = messages[0];
        assertEquals("john.candidate@hireai.test", msg.getAllRecipients()[0].toString());
        assertTrue(msg.getSubject().contains("Principal Cloud Architect"));
        String body = GreenMailUtil.getBody(msg);
        assertTrue(body.contains("Nexus Innovations"));
        assertTrue(body.contains("88%"));
    }

    @Test
    @DisplayName("End-to-end: Password reset token email delivers to user via SMTP")
    void testEndToEnd_PasswordReset() throws Exception {
        emailService.sendPasswordResetEmail(candidateUser, "token-rst-4455", LocalDateTime.now().plusMinutes(15));

        assertTrue(greenMail.waitForIncomingEmail(5000L, 1));
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);

        MimeMessage msg = messages[0];
        assertEquals("Reset Your HireAI Account Password", msg.getSubject());
        String body = GreenMailUtil.getBody(msg);
        assertTrue(body.contains("token-rst-4455"));
        assertTrue(body.contains("http://localhost:5173/reset-password?token=token-rst-4455"));
    }

    @Test
    @DisplayName("End-to-end: Password changed confirmation delivers to user via SMTP")
    void testEndToEnd_PasswordChanged() throws Exception {
        emailService.sendPasswordChangedEmail(candidateUser);

        assertTrue(greenMail.waitForIncomingEmail(5000L, 1));
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);

        MimeMessage msg = messages[0];
        assertEquals("Security Alert: Password Changed - HireAI", msg.getSubject());
        String body = GreenMailUtil.getBody(msg);
        assertTrue(body.contains("Password Successfully Updated"));
    }

    @Test
    @DisplayName("End-to-end: Job posted confirmation delivers to recruiter via SMTP")
    void testEndToEnd_JobPostedConfirmation() throws Exception {
        emailService.sendJobPostedConfirmation(
                recruiterUser.getEmail(), recruiterUser.getFirstName(), "Staff DevOps Engineer", 501L);

        assertTrue(greenMail.waitForIncomingEmail(5000L, 1));
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);

        MimeMessage msg = messages[0];
        assertEquals("sarah.recruiter@hireai.test", msg.getAllRecipients()[0].toString());
        assertTrue(msg.getSubject().contains("Staff DevOps Engineer"));
        String body = GreenMailUtil.getBody(msg);
        assertTrue(body.contains("http://localhost:5173/recruiter/jobs"));
    }

    @Test
    @DisplayName("End-to-end: Account deleted confirmation delivers via SMTP")
    void testEndToEnd_AccountDeleted() throws Exception {
        emailService.sendAccountDeletedEmail("former.user@hireai.test", "Former User");

        assertTrue(greenMail.waitForIncomingEmail(5000L, 1));
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);

        MimeMessage msg = messages[0];
        assertEquals("HireAI Account Deletion Confirmed", msg.getSubject());
        String body = GreenMailUtil.getBody(msg);
        assertTrue(body.contains("Account Deletion Confirmation"));
    }

    @Test
    @DisplayName("End-to-end: Diagnostics test email delivers via SMTP")
    void testEndToEnd_DiagnosticTestEmail() throws Exception {
        emailService.sendTestEmail("admin.diagnostics@hireai.test");

        assertTrue(greenMail.waitForIncomingEmail(5000L, 1));
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);

        MimeMessage msg = messages[0];
        assertEquals("admin.diagnostics@hireai.test", msg.getAllRecipients()[0].toString());
        assertEquals("HireAI Email Delivery Diagnostics - Test Message", msg.getSubject());
        String body = GreenMailUtil.getBody(msg);
        assertTrue(body.contains("Outbound Transport Test Successful"));
    }
}
