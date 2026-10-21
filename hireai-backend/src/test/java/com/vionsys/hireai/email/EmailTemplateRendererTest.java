package com.vionsys.hireai.email;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vionsys.hireai.application.dto.JobApplicationResponse;
import com.vionsys.hireai.application.enums.ApplicationStatus;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.user.entity.User;

class EmailTemplateRendererTest {

    private EmailTemplateRenderer renderer;
    private User testCandidate;
    private User testRecruiter;
    private JobApplicationResponse testApplication;
    private final String baseUrl = "https://app.hireai.com";

    @BeforeEach
    void setUp() {
        renderer = new EmailTemplateRenderer();

        testCandidate = User.builder()
                .id(UUID.randomUUID())
                .firstName("Alice")
                .lastName("Smith")
                .email("alice.smith@example.com")
                .role(Role.builder().id(1L).name(RoleType.ROLE_CANDIDATE).build())
                .build();

        testRecruiter = User.builder()
                .id(UUID.randomUUID())
                .firstName("Bob")
                .lastName("Recruiter")
                .email("bob.recruiter@techcorp.com")
                .role(Role.builder().id(2L).name(RoleType.ROLE_RECRUITER).build())
                .build();

        testApplication = JobApplicationResponse.builder()
                .id(UUID.randomUUID())
                .candidateId(UUID.randomUUID())
                .candidateName("Alice <script>alert('xss')</script> Smith")
                .candidateEmail("alice.smith@example.com")
                .jobId(UUID.randomUUID())
                .jobTitle("Senior Backend Engineer <img src=x onerror=alert(1)>")
                .companyName("TechCorp Inc.")
                .status(ApplicationStatus.SHORTLISTED)
                .atsMatchScore(92)
                .recruiterNotes("Strong Java and microservices portfolio.")
                .build();
    }

    @Test
    @DisplayName("Application received template renders brand colors, escapes inputs, and avoids prohibited styles")
    void testRenderApplicationReceivedCandidate() {
        EmailMessage msg = renderer.renderApplicationReceivedCandidate(testApplication, baseUrl);

        assertNotNull(msg);
        assertEquals("alice.smith@example.com", msg.getTo());
        assertStrictDesignCompliance(msg.getHtmlContent());
        assertFalse(msg.getHtmlContent().contains("<script>"), "HTML injection must be escaped");
        assertTrue(msg.getHtmlContent().contains("&lt;script&gt;"), "Escaped script tags must be present");
        assertTrue(msg.getHtmlContent().contains("https://app.hireai.com/candidate/applications"));
        assertNotNull(msg.getPlainTextContent());
        assertTrue(msg.getPlainTextContent().contains("TechCorp Inc."));
    }

    @Test
    @DisplayName("Recruiter new applicant alert renders properly with escaped inputs and plain text")
    void testRenderNewApplicantAlertToRecruiter() {
        EmailMessage msg = renderer.renderNewApplicantAlertToRecruiter(testApplication, "bob.recruiter@techcorp.com", baseUrl);

        assertNotNull(msg);
        assertEquals("bob.recruiter@techcorp.com", msg.getTo());
        assertStrictDesignCompliance(msg.getHtmlContent());
        assertTrue(msg.getHtmlContent().contains("TechCorp Inc."));
        assertTrue(msg.getHtmlContent().contains("92%"));
        assertTrue(msg.getPlainTextContent().contains("Alice"));
    }

    @Test
    @DisplayName("Status update template renders correct status badge color and recruiter note")
    void testRenderStatusUpdateCandidate() {
        EmailMessage msg = renderer.renderStatusUpdateCandidate(
                testApplication, ApplicationStatus.SHORTLISTED, "Next interview stage confirmed.", baseUrl);

        assertNotNull(msg);
        assertStrictDesignCompliance(msg.getHtmlContent());
        assertTrue(msg.getHtmlContent().contains("SHORTLISTED"));
        assertTrue(msg.getHtmlContent().contains("Next interview stage confirmed."));
        assertTrue(msg.getPlainTextContent().contains("SHORTLISTED"));
    }

    @Test
    @DisplayName("Email verification OTP template renders 6-digit code and security notices")
    void testRenderVerificationOtp() {
        EmailMessage msg = renderer.renderVerificationOtp(testCandidate, "839201", 10);

        assertNotNull(msg);
        assertEquals("alice.smith@example.com", msg.getTo());
        assertStrictDesignCompliance(msg.getHtmlContent());
        assertTrue(msg.getHtmlContent().contains("839201"));
        assertTrue(msg.getHtmlContent().contains("10 minutes"));
        assertEquals("839201", msg.getSensitiveToken());
        assertTrue(msg.getPlainTextContent().contains("839201"));
    }

    @Test
    @DisplayName("Password reset template renders secure token and action link")
    void testRenderPasswordReset() {
        EmailMessage msg = renderer.renderPasswordReset(
                testCandidate, "tok-xyz-987", LocalDateTime.now().plusMinutes(15), baseUrl);

        assertNotNull(msg);
        assertStrictDesignCompliance(msg.getHtmlContent());
        assertTrue(msg.getHtmlContent().contains("tok-xyz-987"));
        assertTrue(msg.getHtmlContent().contains("https://app.hireai.com/reset-password?token=tok-xyz-987"));
        assertEquals("tok-xyz-987", msg.getSensitiveToken());
    }

    @Test
    @DisplayName("Login alert template renders session details without secrets")
    void testRenderLoginAlert() {
        EmailMessage msg = renderer.renderLoginAlert(testCandidate, baseUrl);

        assertNotNull(msg);
        assertStrictDesignCompliance(msg.getHtmlContent());
        assertTrue(msg.getHtmlContent().contains("Security Alert: Account Sign-In"));
        assertTrue(msg.getHtmlContent().contains("alice.smith@example.com"));
        assertTrue(msg.getHtmlContent().contains("https://app.hireai.com/settings"));
    }

    @Test
    @DisplayName("Password changed template renders security alert")
    void testRenderPasswordChanged() {
        EmailMessage msg = renderer.renderPasswordChanged(testCandidate, baseUrl);

        assertNotNull(msg);
        assertStrictDesignCompliance(msg.getHtmlContent());
        assertTrue(msg.getHtmlContent().contains("Password Successfully Updated"));
        assertTrue(msg.getPlainTextContent().contains("changed"));
    }

    @Test
    @DisplayName("Account deleted template renders confirmation")
    void testRenderAccountDeleted() {
        EmailMessage msg = renderer.renderAccountDeleted("deleted.user@example.com", "Deleted User");

        assertNotNull(msg);
        assertEquals("deleted.user@example.com", msg.getTo());
        assertStrictDesignCompliance(msg.getHtmlContent());
        assertTrue(msg.getHtmlContent().contains("Account Deletion Confirmation"));
    }

    @Test
    @DisplayName("Job posted confirmation template renders job details and management link")
    void testRenderJobPostedConfirmation() {
        EmailMessage msg = renderer.renderJobPostedConfirmation(
                "recruiter@hireai.com", "Bob", "Lead Data Engineer", 404L, baseUrl);

        assertNotNull(msg);
        assertEquals("recruiter@hireai.com", msg.getTo());
        assertStrictDesignCompliance(msg.getHtmlContent());
        assertTrue(msg.getHtmlContent().contains("Lead Data Engineer"));
        assertTrue(msg.getHtmlContent().contains("https://app.hireai.com/recruiter/jobs"));
    }

    @Test
    @DisplayName("Sub-admin created template renders onboarding details")
    void testRenderSubAdminCreated() {
        EmailMessage msg = renderer.renderSubAdminCreated(
                "subadmin@hireai.com", "leadadmin@hireai.com", baseUrl);

        assertNotNull(msg);
        assertEquals("subadmin@hireai.com", msg.getTo());
        assertStrictDesignCompliance(msg.getHtmlContent());
        assertTrue(msg.getHtmlContent().contains("Administrative Account Created"));
    }

    @Test
    @DisplayName("Diagnostics test email renders verification message")
    void testRenderTestEmail() {
        EmailMessage msg = renderer.renderTestEmail("admin@hireai.com", baseUrl);

        assertNotNull(msg);
        assertEquals("admin@hireai.com", msg.getTo());
        assertStrictDesignCompliance(msg.getHtmlContent());
        assertTrue(msg.getHtmlContent().contains("Outbound Transport Test Successful"));
    }

    private void assertEquals(String expected, String actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    /**
     * Enforce strict AGENTS.md compliance:
     * - Official brand blues (#004499, #0192C6, #112358)
     * - No purple gradients or hex values (#6D3DF5, #C63FC5)
     * - No emoji icons
     * - No em dashes (—)
     * - No pill buttons (rounded-full)
     */
    private void assertStrictDesignCompliance(String html) {
        assertNotNull(html);
        assertFalse(html.contains("#6D3DF5"), "Prohibited purple hex #6D3DF5 found");
        assertFalse(html.contains("#C63FC5"), "Prohibited magenta hex #C63FC5 found");
        assertFalse(html.contains("purple"), "Prohibited purple CSS found");
        assertFalse(html.contains("—"), "Prohibited em dash found; use standard hyphen (-)");
        assertFalse(html.contains("rounded-full"), "Prohibited pill-shaped button found; use rounded-lg or rounded-xl");
        assertFalse(html.contains("✨"), "Prohibited emoji found");
        assertFalse(html.contains("🚀"), "Prohibited emoji found");
        assertFalse(html.contains("🤖"), "Prohibited emoji found");
        assertFalse(html.contains("🎉"), "Prohibited emoji found");
    }
}
