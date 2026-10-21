package com.vionsys.hireai.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.vionsys.hireai.exception.EmailDeliveryException;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailTransportTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private Environment environment;

    private EmailProperties emailProperties;
    private SmtpEmailTransport smtpTransport;
    private LogEmailTransport logTransport;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setEnabled(true);
        emailProperties.setFrom("notifications@vionsys.com");
        emailProperties.setSenderName("HireAI Platform");
        emailProperties.setMaxRetries(3);
        emailProperties.setRetryDelayMs(50L); // fast backoff for unit tests

        smtpTransport = new SmtpEmailTransport(mailSenderProvider, emailProperties);
        logTransport = new LogEmailTransport(emailProperties, environment);
    }

    @Test
    @DisplayName("SMTP transport: retries transient network errors up to maxRetries")
    void testSmtpTransport_RetriesTransientErrors() {
        JavaMailSenderImpl sender = mock(JavaMailSenderImpl.class);
        when(sender.getPassword()).thenReturn("configured-pwd");
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailSenderProvider.getIfAvailable()).thenReturn(sender);

        // Fail first 2 attempts with transient error, succeed on 3rd attempt
        doThrow(new MailSendException("Socket timeout"))
                .doThrow(new MailSendException("Connection reset"))
                .doNothing()
                .when(sender).send(any(MimeMessage.class));

        EmailMessage message = EmailMessage.builder()
                .to("candidate@example.com")
                .subject("Test Subject")
                .htmlContent("<p>Hello</p>")
                .plainTextContent("Hello")
                .emailType("TEST")
                .correlationId("corr-123")
                .build();

        smtpTransport.dispatch(message);

        verify(sender, times(3)).send(any(MimeMessage.class));
        assertEquals(1, smtpTransport.getTotalSentCount());
        assertEquals(0, smtpTransport.getRecentFailureCount());
        assertNotNull(smtpTransport.getLastSuccessfulSend());
    }

    @Test
    @DisplayName("SMTP transport: permanent authentication failure fails fast with zero retries")
    void testSmtpTransport_PermanentAuthFailure_NoRetry() {
        JavaMailSenderImpl sender = mock(JavaMailSenderImpl.class);
        when(sender.getPassword()).thenReturn("configured-pwd");
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailSenderProvider.getIfAvailable()).thenReturn(sender);

        doThrow(new MailAuthenticationException("535 5.7.8 Username and Password not accepted"))
                .when(sender).send(any(MimeMessage.class));

        EmailMessage message = EmailMessage.builder()
                .to("candidate@example.com")
                .subject("Test Subject")
                .htmlContent("<p>Hello</p>")
                .plainTextContent("Hello")
                .emailType("TEST")
                .correlationId("corr-auth")
                .build();

        assertThrows(EmailDeliveryException.class, () -> smtpTransport.dispatch(message));

        // Must verify send was only attempted once (NO RETRY on permanent auth failure)
        verify(sender, times(1)).send(any(MimeMessage.class));
        assertEquals(1, smtpTransport.getTotalFailedCount());
        assertEquals(1, smtpTransport.getRecentFailureCount());
    }

    @Test
    @DisplayName("SMTP transport: rejects malformed recipient address before network call")
    void testSmtpTransport_MalformedRecipient_FailsFast() {
        EmailMessage message = EmailMessage.builder()
                .to("invalid-address-without-at-sign")
                .subject("Test Subject")
                .htmlContent("<p>Hello</p>")
                .plainTextContent("Hello")
                .emailType("TEST")
                .correlationId("corr-malformed")
                .build();

        assertThrows(EmailDeliveryException.class, () -> smtpTransport.dispatch(message));
        assertEquals(1, smtpTransport.getTotalFailedCount());
    }

    @Test
    @DisplayName("SMTP transport: isReady correctly checks password configuration")
    void testSmtpTransport_IsReady() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setPassword("");
        when(mailSenderProvider.getIfAvailable()).thenReturn(sender);

        assertFalse(smtpTransport.isReady());

        sender.setPassword("valid-password");
        assertTrue(smtpTransport.isReady());
    }

    @Test
    @DisplayName("LOG transport: successfully logs dispatch and increments counters")
    void testLogTransport_DispatchSuccess() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        EmailMessage message = EmailMessage.builder()
                .to("developer@example.com")
                .subject("Dev Alert")
                .htmlContent("<p>Dev Mode</p>")
                .plainTextContent("Dev Mode")
                .emailType("DEV_TEST")
                .correlationId("corr-log")
                .sensitiveToken("SECRET-999")
                .build();

        logTransport.dispatch(message);

        assertEquals(1, logTransport.getTotalSentCount());
        assertEquals(0, logTransport.getRecentFailureCount());
        assertNotNull(logTransport.getLastSuccessfulSend());
        assertTrue(logTransport.isReady());
        assertEquals(EmailMode.LOG, logTransport.getMode());
    }
}
