package com.vionsys.hireai.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.exception.EmailDeliveryException;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.user.entity.User;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private Environment environment;

    private EmailProperties emailProperties;
    private EmailServiceImpl emailService;

    private User testUser;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setEnabled(true);
        emailProperties.setFrom("notifications@vionsys.com");
        emailProperties.setSenderName("HireAI Platform");

        emailService = new EmailServiceImpl(mailSenderProvider, emailProperties, environment);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .role(Role.builder().id(1L).name(RoleType.ROLE_CANDIDATE).build())
                .build();
    }

    @Test
    void testMaskEmail_Hygiene() {
        assertEquals("j***e@example.com", EmailServiceImpl.maskEmail("john.doe@example.com"));
        assertEquals("a***x@example.com", EmailServiceImpl.maskEmail("alex@example.com"));
        assertEquals("m***2@gmail.com", EmailServiceImpl.maskEmail("mohitpc2002@gmail.com"));
        assertEquals("***", EmailServiceImpl.maskEmail(null));
        assertEquals("***", EmailServiceImpl.maskEmail(""));
    }

    @Test
    void testIsSmtpConfigured_ReturnsFalse_WhenPasswordBlank() {
        JavaMailSenderImpl senderImpl = new JavaMailSenderImpl();
        senderImpl.setHost("smtp.gmail.com");
        senderImpl.setUsername("user@example.com");
        senderImpl.setPassword(""); // empty password
        when(mailSenderProvider.getIfAvailable()).thenReturn(senderImpl);

        assertFalse(emailService.isSmtpConfigured());
    }

    @Test
    void testIsSmtpConfigured_ReturnsTrue_WhenPasswordProvided() {
        JavaMailSenderImpl senderImpl = new JavaMailSenderImpl();
        senderImpl.setHost("smtp.gmail.com");
        senderImpl.setUsername("user@example.com");
        senderImpl.setPassword("superSecretAppPassword");
        when(mailSenderProvider.getIfAvailable()).thenReturn(senderImpl);

        assertTrue(emailService.isSmtpConfigured());
    }

    @Test
    void testSendEmailVerificationOtp_SimulatedMode_WhenSmtpUnconfigured() {
        // Mail sender has empty password -> simulation mode
        JavaMailSenderImpl senderImpl = new JavaMailSenderImpl();
        senderImpl.setPassword("");
        when(mailSenderProvider.getIfAvailable()).thenReturn(senderImpl);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        // Should not throw, should run in simulation mode
        emailService.sendEmailVerificationOtp(testUser, "123456", 10);
        assertFalse(emailService.isSmtpConfigured());
    }

    @Test
    void testSendEmailVerificationOtp_Success_WhenSmtpConfigured() {
        JavaMailSenderImpl senderImpl = mock(JavaMailSenderImpl.class);
        when(senderImpl.getPassword()).thenReturn("app-password-configured");
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(senderImpl.createMimeMessage()).thenReturn(mimeMessage);
        when(mailSenderProvider.getIfAvailable()).thenReturn(senderImpl);

        emailService.sendEmailVerificationOtp(testUser, "654321", 10);

        verify(senderImpl).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailVerificationOtp_ThrowsEmailDeliveryException_OnSmtpAuthFailure() {
        JavaMailSenderImpl senderImpl = mock(JavaMailSenderImpl.class);
        when(senderImpl.getPassword()).thenReturn("configured-but-invalid");
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(senderImpl.createMimeMessage()).thenReturn(mimeMessage);
        when(mailSenderProvider.getIfAvailable()).thenReturn(senderImpl);

        doThrow(new MailAuthenticationException("535 Authentication Failed"))
                .when(senderImpl).send(any(MimeMessage.class));

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () ->
                emailService.sendEmailVerificationOtp(testUser, "654321", 10)
        );

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("We could not send the verification email"));
    }

    @Test
    void testSendPasswordResetEmail_ThrowsEmailDeliveryException_OnSmtpFailure() {
        JavaMailSenderImpl senderImpl = mock(JavaMailSenderImpl.class);
        when(senderImpl.getPassword()).thenReturn("valid-format-pwd");
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(senderImpl.createMimeMessage()).thenReturn(mimeMessage);
        when(mailSenderProvider.getIfAvailable()).thenReturn(senderImpl);

        doThrow(new MailAuthenticationException("Connection timed out"))
                .when(senderImpl).send(any(MimeMessage.class));

        assertThrows(EmailDeliveryException.class, () ->
                emailService.sendPasswordResetEmail(testUser, "reset-token-123", LocalDateTime.now().plusMinutes(15))
        );
    }

    @Test
    void testNonCriticalWelcomeEmail_DoesNotThrow_OnSmtpFailure() {
        JavaMailSenderImpl senderImpl = mock(JavaMailSenderImpl.class);
        when(senderImpl.getPassword()).thenReturn("valid-format-pwd");
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(senderImpl.createMimeMessage()).thenReturn(mimeMessage);
        when(mailSenderProvider.getIfAvailable()).thenReturn(senderImpl);

        doThrow(new MailAuthenticationException("SMTP connection refused"))
                .when(senderImpl).send(any(MimeMessage.class));

        // Non-critical welcome email should swallow error gracefully and log warning without crashing
        emailService.sendWelcomeCandidateEmail(testUser);
        verify(senderImpl).send(any(MimeMessage.class));
    }
}
