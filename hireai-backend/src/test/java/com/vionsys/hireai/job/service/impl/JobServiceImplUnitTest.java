package com.vionsys.hireai.job.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vionsys.hireai.application.repository.JobApplicationRepository;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.email.EmailService;
import com.vionsys.hireai.exception.JobNotFoundException;
import com.vionsys.hireai.exception.RecruiterProfileNotFoundException;
import com.vionsys.hireai.job.dto.JobRequest;
import com.vionsys.hireai.job.dto.JobResponse;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.enums.Currency;
import com.vionsys.hireai.job.enums.EmploymentType;
import com.vionsys.hireai.job.enums.ExperienceLevel;
import com.vionsys.hireai.job.enums.JobStatus;
import com.vionsys.hireai.job.repository.JobRepository;
import com.vionsys.hireai.notification.service.NotificationService;
import com.vionsys.hireai.recruiter.entity.RecruiterProfile;
import com.vionsys.hireai.recruiter.repository.RecruiterProfileRepository;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.user.entity.User;

@ExtendWith(MockitoExtension.class)
class JobServiceImplUnitTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private RecruiterProfileRepository recruiterProfileRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private JobServiceImpl jobService;

    private UUID recruiterUserId;
    private UUID recruiterProfileId;
    private RecruiterProfile recruiterProfile;
    private CustomUserDetails recruiterUserDetails;
    private Job job;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        recruiterUserId = UUID.randomUUID();
        recruiterProfileId = UUID.randomUUID();
        jobId = UUID.randomUUID();

        User user = User.builder()
                .id(recruiterUserId)
                .email("recruiter@techcorp.com")
                .firstName("Sarah")
                .lastName("Recruiter")
                .role(Role.builder().id(2L).name(RoleType.ROLE_RECRUITER).build())
                .build();

        recruiterProfile = RecruiterProfile.builder()
                .id(recruiterProfileId)
                .user(user)
                .companyName("TechCorp")
                .companyEmail("jobs@techcorp.com")
                .build();

        recruiterUserDetails = CustomUserDetails.fromUser(user, RoleType.ROLE_RECRUITER);

        job = Job.builder()
                .id(jobId)
                .title("Senior Backend Engineer")
                .description("Build high-performance REST APIs in Java/Spring")
                .employmentType(EmploymentType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR)
                .location("Bangalore")
                .remote(true)
                .salaryMin(new BigDecimal("1800000"))
                .salaryMax(new BigDecimal("2800000"))
                .currency(Currency.INR)
                .skills(List.of("Java", "Spring Boot", "PostgreSQL"))
                .preferredSkills(List.of("Docker", "Kubernetes"))
                .openings(3)
                .applicationDeadline(LocalDate.now().plusMonths(1))
                .status(JobStatus.OPEN)
                .recruiterProfile(recruiterProfile)
                .build();

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockCurrentRecruiter() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(recruiterUserDetails);
        when(recruiterProfileRepository.findByUserId(recruiterUserId)).thenReturn(Optional.of(recruiterProfile));
    }

    // =========================================================================
    // 1. CREATE JOB
    // =========================================================================

    @Test
    @DisplayName("shouldCreateJobSuccessfully_whenRecruiterIsValid")
    void shouldCreateJobSuccessfully_whenRecruiterIsValid() {
        // Arrange
        mockCurrentRecruiter();

        JobRequest request = JobRequest.builder()
                .title("Senior Backend Engineer")
                .description("Build high-performance REST APIs in Java/Spring")
                .employmentType(EmploymentType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR)
                .location("Bangalore")
                .remote(true)
                .salaryMin(new BigDecimal("1800000"))
                .salaryMax(new BigDecimal("2800000"))
                .currency(Currency.INR)
                .skills(List.of("Java", "Spring Boot", "PostgreSQL"))
                .preferredSkills(List.of("Docker", "Kubernetes"))
                .openings(3)
                .applicationDeadline(LocalDate.now().plusMonths(1))
                .build();

        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            j.setId(jobId);
            return j;
        });

        // Act
        JobResponse response = jobService.createJob(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Senior Backend Engineer");
        assertThat(response.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(response.getCompanyName()).isEqualTo("TechCorp");
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    @DisplayName("shouldThrowRecruiterProfileNotFoundException_whenRecruiterProfileMissing")
    void shouldThrowRecruiterProfileNotFoundException_whenRecruiterProfileMissing() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(recruiterUserDetails);
        when(recruiterProfileRepository.findByUserId(recruiterUserId)).thenReturn(Optional.empty());

        JobRequest request = JobRequest.builder().title("Engineer").build();

        // Act & Assert
        assertThrows(RecruiterProfileNotFoundException.class, () -> jobService.createJob(request));
        verify(jobRepository, never()).save(any());
    }

    // =========================================================================
    // 2. UPDATE JOB & OWNERSHIP CHECKS
    // =========================================================================

    @Test
    @DisplayName("shouldUpdateJobSuccessfully_whenRecruiterIsOwner")
    void shouldUpdateJobSuccessfully_whenRecruiterIsOwner() {
        // Arrange
        mockCurrentRecruiter();

        JobRequest updateRequest = JobRequest.builder()
                .title("Lead Backend Engineer")
                .description("Updated description")
                .employmentType(EmploymentType.FULL_TIME)
                .experienceLevel(ExperienceLevel.LEAD)
                .location("Hyderabad")
                .remote(true)
                .salaryMin(new BigDecimal("3000000"))
                .salaryMax(new BigDecimal("4500000"))
                .currency(Currency.INR)
                .skills(List.of("Java", "Distributed Systems"))
                .openings(2)
                .status(JobStatus.OPEN)
                .build();

        when(jobRepository.findByIdAndRecruiterProfileId(jobId, recruiterProfileId))
                .thenReturn(Optional.of(job));

        // Act
        JobResponse response = jobService.updateJob(jobId, updateRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(job.getTitle()).isEqualTo("Lead Backend Engineer");
        assertThat(job.getExperienceLevel()).isEqualTo(ExperienceLevel.LEAD);
        assertThat(job.getLocation()).isEqualTo("Hyderabad");
    }

    @Test
    @DisplayName("shouldPauseJob_whenStatusSetToPausedInUpdateRequest")
    void shouldPauseJob_whenStatusSetToPausedInUpdateRequest() {
        // Arrange
        mockCurrentRecruiter();

        JobRequest pauseRequest = JobRequest.builder()
                .title("Senior Backend Engineer")
                .description("Build high-performance REST APIs")
                .employmentType(EmploymentType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR)
                .location("Bangalore")
                .remote(true)
                .status(JobStatus.PAUSED)
                .build();

        when(jobRepository.findByIdAndRecruiterProfileId(jobId, recruiterProfileId))
                .thenReturn(Optional.of(job));

        // Act
        JobResponse response = jobService.updateJob(jobId, pauseRequest);

        // Assert
        assertThat(job.getStatus()).isEqualTo(JobStatus.PAUSED);
        assertThat(response.getStatus()).isEqualTo(JobStatus.PAUSED);
    }

    @Test
    @DisplayName("shouldThrowJobNotFoundException_whenNonOwnerRecruiterAttemptsUpdate")
    void shouldThrowJobNotFoundException_whenNonOwnerRecruiterAttemptsUpdate() {
        // Arrange: Job does not belong to this recruiterProfileId
        mockCurrentRecruiter();

        when(jobRepository.findByIdAndRecruiterProfileId(jobId, recruiterProfileId))
                .thenReturn(Optional.empty());

        JobRequest updateRequest = JobRequest.builder().title("Hacked Title").build();

        // Act & Assert
        JobNotFoundException ex = assertThrows(JobNotFoundException.class,
                () -> jobService.updateJob(jobId, updateRequest));
        assertThat(ex.getMessage()).isEqualTo("Job not found");
    }

    // =========================================================================
    // 3. CLOSE JOB
    // =========================================================================

    @Test
    @DisplayName("shouldCloseJobSuccessfully_whenOwnerRecruiterClosesJob")
    void shouldCloseJobSuccessfully_whenOwnerRecruiterClosesJob() {
        // Arrange
        mockCurrentRecruiter();

        when(jobRepository.findByIdAndRecruiterProfileId(jobId, recruiterProfileId))
                .thenReturn(Optional.of(job));

        // Act
        jobService.closeJob(jobId);

        // Assert
        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    @DisplayName("shouldThrowJobNotFoundException_whenNonOwnerAttemptsClose")
    void shouldThrowJobNotFoundException_whenNonOwnerAttemptsClose() {
        // Arrange: Not found for this recruiter
        mockCurrentRecruiter();

        when(jobRepository.findByIdAndRecruiterProfileId(jobId, recruiterProfileId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(JobNotFoundException.class, () -> jobService.closeJob(jobId));
    }
}

