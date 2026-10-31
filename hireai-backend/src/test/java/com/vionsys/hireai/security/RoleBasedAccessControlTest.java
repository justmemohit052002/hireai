package com.vionsys.hireai.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vionsys.hireai.application.controller.JobApplicationController;
import com.vionsys.hireai.application.service.JobApplicationService;
import com.vionsys.hireai.job.controller.JobController;
import com.vionsys.hireai.job.dto.JobRequest;
import com.vionsys.hireai.job.service.JobService;
import com.vionsys.hireai.security.evaluator.JobSecurityEvaluator;
import com.vionsys.hireai.security.jwt.JwtAccessDeniedHandler;
import com.vionsys.hireai.security.jwt.JwtAuthenticationEntryPoint;
import com.vionsys.hireai.security.jwt.JwtAuthenticationFilter;
import com.vionsys.hireai.security.ratelimit.RateLimitingFilter;
import com.vionsys.hireai.security.resolver.CurrentUserArgumentResolver;

import jakarta.servlet.FilterChain;

@WebMvcTest(controllers = {JobController.class, JobApplicationController.class})
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, CurrentUserArgumentResolver.class})
@ActiveProfiles("test")
class RoleBasedAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobService jobService;

    @MockBean
    private JobApplicationService jobApplicationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private MustChangePasswordFilter mustChangePasswordFilter;

    @MockBean(name = "jobSecurity")
    private JobSecurityEvaluator jobSecurityEvaluator;

    private UUID sampleJobId;

    @BeforeEach
    void setUp() throws Exception {
        sampleJobId = UUID.randomUUID();

        // Allow filters to delegate to next filter in chain
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(mustChangePasswordFilter).doFilter(any(), any(), any());
    }

    // =========================================================================
    // 1. UNAUTHENTICATED REQUESTS (NO TOKEN) -> 401 UNAUTHORIZED
    // =========================================================================

    @Test
    @DisplayName("shouldReturn401_whenNoTokenAccessesRecruiterJobsEndpoint")
    void shouldReturn401_whenNoTokenAccessesRecruiterJobsEndpoint() throws Exception {
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("shouldReturn401_whenNoTokenAccessesApplyEndpoint")
    void shouldReturn401_whenNoTokenAccessesApplyEndpoint() throws Exception {
        mockMvc.perform(post("/jobs/" + sampleJobId + "/apply")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("shouldReturn401_whenNoTokenAccessesJobCreationEndpoint")
    void shouldReturn401_whenNoTokenAccessesJobCreationEndpoint() throws Exception {
        JobRequest request = JobRequest.builder().title("Engineer").build();
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 2. CANDIDATE ACCESSING RECRUITER ENDPOINTS -> 403 FORBIDDEN
    // =========================================================================

    @Test
    @WithMockUser(username = "candidate@hireai.dev", authorities = {"ROLE_CANDIDATE", "application:create", "application:read"})
    @DisplayName("shouldReturn403_whenCandidateAttemptsToAccessRecruiterMyJobs")
    void shouldReturn403_whenCandidateAttemptsToAccessRecruiterMyJobs() throws Exception {
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "candidate@hireai.dev", authorities = {"ROLE_CANDIDATE", "application:create", "application:read"})
    @DisplayName("shouldReturn403_whenCandidateAttemptsToCreateJob")
    void shouldReturn403_whenCandidateAttemptsToCreateJob() throws Exception {
        JobRequest request = JobRequest.builder().title("Engineer").build();
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. RECRUITER ACCESSING CANDIDATE-ONLY ENDPOINTS -> 403 FORBIDDEN
    // =========================================================================

    @Test
    @WithMockUser(username = "recruiter@hireai.dev", authorities = {"ROLE_RECRUITER", "job:create", "job:read", "job:update", "job:delete"})
    @DisplayName("shouldReturn403_whenRecruiterAttemptsToApplyToJob")
    void shouldReturn403_whenRecruiterAttemptsToApplyToJob() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "%PDF".getBytes());
        mockMvc.perform(multipart("/jobs/" + sampleJobId + "/apply")
                        .file(file))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 4. AUTHORIZED ROLE ACCESS -> 200 OK
    // =========================================================================

    @Test
    @WithMockUser(username = "candidate@hireai.dev", authorities = {"ROLE_CANDIDATE"})
    @DisplayName("shouldAllowCandidateToBrowseOpenJobs")
    void shouldAllowCandidateToBrowseOpenJobs() throws Exception {
        when(jobService.getOpenJobs()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/jobs/open"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "recruiter@hireai.dev", authorities = {"ROLE_RECRUITER", "job:read"})
    @DisplayName("shouldAllowRecruiterToBrowseOpenJobsAndOwnJobs")
    void shouldAllowRecruiterToBrowseOpenJobsAndOwnJobs() throws Exception {
        when(jobService.getOpenJobs()).thenReturn(Collections.emptyList());
        when(jobService.getMyJobs()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/jobs/open"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk());
    }
}
