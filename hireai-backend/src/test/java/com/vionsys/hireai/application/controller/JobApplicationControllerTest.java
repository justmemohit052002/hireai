package com.vionsys.hireai.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vionsys.hireai.application.dto.JobApplicationRequest;
import com.vionsys.hireai.application.dto.JobApplicationResponse;
import com.vionsys.hireai.application.dto.UpdateApplicationStatusRequest;
import com.vionsys.hireai.application.enums.ApplicationStatus;
import com.vionsys.hireai.application.service.JobApplicationService;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.security.jwt.JwtAccessDeniedHandler;
import com.vionsys.hireai.security.jwt.JwtAuthenticationEntryPoint;
import com.vionsys.hireai.security.jwt.JwtAuthenticationFilter;
import com.vionsys.hireai.security.jwt.JwtService;

@WebMvcTest(JobApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class JobApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobApplicationService jobApplicationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(
                userId,
                "candidate@example.com",
                "encodedPassword",
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @Test
    void testApplyToJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        JobApplicationRequest request = new JobApplicationRequest();

        JobApplicationResponse response = JobApplicationResponse.builder()
                .id(UUID.randomUUID())
                .jobId(jobId)
                .jobTitle("Backend Engineer")
                .status(ApplicationStatus.APPLIED)
                .atsMatchScore(85)
                .build();

        when(jobApplicationService.applyToJob(eq(userId), eq(jobId), any())).thenReturn(response);

        mockMvc.perform(post("/jobs/" + jobId + "/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.jobTitle").value("Backend Engineer"))
                .andExpect(jsonPath("$.data.atsMatchScore").value(85));
    }

    @Test
    void testGetMyApplications() throws Exception {
        JobApplicationResponse response = JobApplicationResponse.builder()
                .id(UUID.randomUUID())
                .jobTitle("Frontend Engineer")
                .status(ApplicationStatus.SHORTLISTED)
                .build();

        when(jobApplicationService.getCandidateApplications(eq(userId))).thenReturn(List.of(response));

        mockMvc.perform(get("/candidate/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].jobTitle").value("Frontend Engineer"))
                .andExpect(jsonPath("$.data[0].status").value("SHORTLISTED"));
    }

    @Test
    void testUpdateApplicationStatus() throws Exception {
        UUID appId = UUID.randomUUID();
        UpdateApplicationStatusRequest request = UpdateApplicationStatusRequest.builder()
                .status(ApplicationStatus.SHORTLISTED)
                .build();

        JobApplicationResponse response = JobApplicationResponse.builder()
                .id(appId)
                .status(ApplicationStatus.SHORTLISTED)
                .build();

        when(jobApplicationService.updateApplicationStatus(eq(userId), eq(appId), any())).thenReturn(response);

        mockMvc.perform(patch("/applications/" + appId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHORTLISTED"));
    }
}
