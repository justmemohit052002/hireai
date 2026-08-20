package com.vionsys.hireai.candidate.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.vionsys.hireai.candidate.dto.ResumeResponse;
import com.vionsys.hireai.candidate.enums.ResumeStatus;
import com.vionsys.hireai.candidate.service.ResumeService;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.security.jwt.JwtAccessDeniedHandler;
import com.vionsys.hireai.security.jwt.JwtAuthenticationEntryPoint;
import com.vionsys.hireai.security.jwt.JwtAuthenticationFilter;
import com.vionsys.hireai.security.jwt.JwtService;

@WebMvcTest(ResumeController.class)
@AutoConfigureMockMvc(addFilters = false)
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeService resumeService;

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
    void testUploadResume_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "Mock PDF Resume Content".getBytes()
        );

        ResumeResponse response = ResumeResponse.builder()
                .id(UUID.randomUUID())
                .originalFileName("resume.pdf")
                .fileType("application/pdf")
                .resumeStatus(ResumeStatus.PARSING)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(resumeService.uploadMyResume(eq(userId), any())).thenReturn(response);

        mockMvc.perform(multipart("/candidate/resume/upload").file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.originalFileName").value("resume.pdf"))
                .andExpect(jsonPath("$.data.resumeStatus").value("PARSING"));
    }

    @Test
    void testGetMyResume_Success() throws Exception {
        ResumeResponse response = ResumeResponse.builder()
                .id(UUID.randomUUID())
                .originalFileName("my_resume.pdf")
                .fileType("application/pdf")
                .resumeStatus(ResumeStatus.PARSED)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(resumeService.getMyResume(eq(userId))).thenReturn(response);

        mockMvc.perform(get("/candidate/resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalFileName").value("my_resume.pdf"))
                .andExpect(jsonPath("$.data.resumeStatus").value("PARSED"));
    }

    @Test
    void testDeleteMyResume_Success() throws Exception {
        mockMvc.perform(delete("/candidate/resume"))
                .andExpect(status().isNoContent());
    }
}
