package com.vionsys.hireai.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vionsys.hireai.application.config.AtsProperties;
import com.vionsys.hireai.application.dto.AtsMatchResult;
import com.vionsys.hireai.application.dto.JobApplicationRequest;
import com.vionsys.hireai.application.dto.JobApplicationResponse;
import com.vionsys.hireai.application.entity.JobApplication;
import com.vionsys.hireai.application.enums.ApplicationStatus;
import com.vionsys.hireai.application.repository.JobApplicationRepository;
import com.vionsys.hireai.application.service.AtsMatchScoringService;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.exception.DuplicateResourceException;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.candidate.service.ResumeService;
import com.vionsys.hireai.candidate.util.CandidateIdGenerator;
import com.vionsys.hireai.email.EmailService;
import com.vionsys.hireai.exception.JobNotFoundException;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.enums.JobStatus;
import com.vionsys.hireai.job.repository.JobRepository;
import com.vionsys.hireai.notification.service.NotificationService;
import com.vionsys.hireai.recruiter.entity.RecruiterProfile;
import com.vionsys.hireai.recruiter.repository.RecruiterProfileRepository;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceImplUnitTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CandidateIdGenerator candidateIdGenerator;

    @Mock
    private AtsMatchScoringService atsMatchScoringService;

    @Mock
    private AtsProperties atsProperties;

    @Mock
    private ResumeService resumeService;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RecruiterProfileRepository recruiterProfileRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private JobApplicationServiceImpl jobApplicationService;

    private UUID candidateUserId;
    private UUID candidateId;
    private UUID recruiterUserId;
    private UUID jobId;
    private Candidate candidate;
    private Job openJob;
    private RecruiterProfile recruiterProfile;
    private User recruiterUser;

    @BeforeEach
    void setUp() {
        candidateUserId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        recruiterUserId = UUID.randomUUID();
        jobId = UUID.randomUUID();

        User candidateUser = User.builder()
                .id(candidateUserId)
                .email("candidate@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        candidate = Candidate.builder()
                .id(candidateId)
                .candidateId("CAND-001")
                .user(candidateUser)
                .build();

        recruiterUser = User.builder()
                .id(recruiterUserId)
                .email("recruiter@innovate.com")
                .firstName("Jane")
                .lastName("Recruiter")
                .build();

        recruiterProfile = RecruiterProfile.builder()
                .id(UUID.randomUUID())
                .user(recruiterUser)
                .companyName("InnovateCorp")
                .build();

        openJob = Job.builder()
                .id(jobId)
                .title("Full Stack Developer")
                .status(JobStatus.OPEN)
                .recruiterProfile(recruiterProfile)
                .build();

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockActiveCandidate() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenAnswer(i -> List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")));
    }

    // =========================================================================
    // 1. SUCCESSFUL APPLICATION
    // =========================================================================

    @Test
    @DisplayName("shouldApplyToJobSuccessfully_whenCandidateAndJobAreValid")
    void shouldApplyToJobSuccessfully_whenCandidateAndJobAreValid() {
        // Arrange
        mockActiveCandidate();
        JobApplicationRequest request = JobApplicationRequest.builder()
                .coverNote("I am an experienced developer interested in this role.")
                .build();

        when(candidateRepository.findByUserId(candidateUserId)).thenReturn(Optional.of(candidate));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(openJob));
        when(recruiterProfileRepository.findByUserId(candidateUserId)).thenReturn(Optional.empty());
        when(jobApplicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)).thenReturn(false);

        AtsMatchResult matchResult = AtsMatchResult.builder()
                .matchScore(85)
                .matchingSkills(List.of("Java", "React"))
                .missingSkills(List.of())
                .relatedSkills(List.of())
                .build();
        when(atsMatchScoringService.computeAtsScore(eq(candidate), eq(openJob))).thenReturn(matchResult);

        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(invocation -> {
            JobApplication app = invocation.getArgument(0);
            app.setId(UUID.randomUUID());
            return app;
        });

        // Act
        JobApplicationResponse response = jobApplicationService.applyToJob(candidateUserId, jobId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);
        assertThat(response.getAtsMatchScore()).isEqualTo(85);
        verify(jobApplicationRepository).save(any(JobApplication.class));
    }

    // =========================================================================
    // 2. REJECT WHEN ACTIVE ROLE IS NOT CANDIDATE
    // =========================================================================

    @Test
    @DisplayName("shouldThrowAccessDeniedException_whenActiveRoleIsNotCandidate")
    void shouldThrowAccessDeniedException_whenActiveRoleIsNotCandidate() {
        // Arrange: User has active role ROLE_RECRUITER
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenAnswer(i -> List.of(new SimpleGrantedAuthority("ROLE_RECRUITER")));

        JobApplicationRequest request = JobApplicationRequest.builder().build();

        // Act & Assert
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> jobApplicationService.applyToJob(candidateUserId, jobId, request));
        assertThat(ex.getMessage()).contains("Only users with an active CANDIDATE role can apply for jobs.");
        verify(jobApplicationRepository, never()).save(any());
    }

    // =========================================================================
    // 3. REJECT DUPLICATE APPLICATION
    // =========================================================================

    @Test
    @DisplayName("shouldThrowDuplicateResourceException_whenCandidateAlreadyApplied")
    void shouldThrowDuplicateResourceException_whenCandidateAlreadyApplied() {
        // Arrange
        mockActiveCandidate();
        JobApplicationRequest request = JobApplicationRequest.builder().build();

        when(candidateRepository.findByUserId(candidateUserId)).thenReturn(Optional.of(candidate));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(openJob));
        when(recruiterProfileRepository.findByUserId(candidateUserId)).thenReturn(Optional.empty());
        when(jobApplicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)).thenReturn(true);

        // Act & Assert
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> jobApplicationService.applyToJob(candidateUserId, jobId, request));
        assertThat(ex.getMessage()).contains("You have already submitted an application for this job posting.");
        verify(jobApplicationRepository, never()).save(any());
    }

    // =========================================================================
    // 4. REJECT APPLICATION ON CLOSED OR PAUSED JOBS
    // =========================================================================

    @Test
    @DisplayName("shouldThrowIllegalArgumentException_whenJobIsClosed")
    void shouldThrowIllegalArgumentException_whenJobIsClosed() {
        // Arrange
        mockActiveCandidate();
        openJob.setStatus(JobStatus.CLOSED);

        when(candidateRepository.findByUserId(candidateUserId)).thenReturn(Optional.of(candidate));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(openJob));

        JobApplicationRequest request = JobApplicationRequest.builder().build();

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> jobApplicationService.applyToJob(candidateUserId, jobId, request));
        assertThat(ex.getMessage()).contains("currently closed or paused and not accepting new applications");
    }

    @Test
    @DisplayName("shouldThrowIllegalArgumentException_whenJobIsPaused")
    void shouldThrowIllegalArgumentException_whenJobIsPaused() {
        // Arrange
        mockActiveCandidate();
        openJob.setStatus(JobStatus.PAUSED);

        when(candidateRepository.findByUserId(candidateUserId)).thenReturn(Optional.of(candidate));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(openJob));

        JobApplicationRequest request = JobApplicationRequest.builder().build();

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> jobApplicationService.applyToJob(candidateUserId, jobId, request));
        assertThat(ex.getMessage()).contains("currently closed or paused and not accepting new applications");
    }

    // =========================================================================
    // 5. SELF-APPLY PROTECTION (OWN JOB OR SAME COMPANY)
    // =========================================================================

    @Test
    @DisplayName("shouldRejectApplication_whenRecruiterAppliesToOwnJob")
    void shouldRejectApplication_whenRecruiterAppliesToOwnJob() {
        // Arrange: candidateUserId is the same recruiter user who posted the job
        mockActiveCandidate();
        recruiterProfile.getUser().setId(candidateUserId);

        when(candidateRepository.findByUserId(candidateUserId)).thenReturn(Optional.of(candidate));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(openJob));

        JobApplicationRequest request = JobApplicationRequest.builder().build();

        // Act & Assert
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> jobApplicationService.applyToJob(candidateUserId, jobId, request));
        assertThat(ex.getMessage()).contains("You cannot apply to a job posted by you or your company.");
        verify(jobApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldRejectApplication_whenCandidateBelongsToSameCompanyAsJob")
    void shouldRejectApplication_whenCandidateBelongsToSameCompanyAsJob() {
        // Arrange: Different user IDs, but candidate has recruiter profile for InnovateCorp
        mockActiveCandidate();
        RecruiterProfile candidateRecruiterProfile = RecruiterProfile.builder()
                .id(UUID.randomUUID())
                .companyName("InnovateCorp")
                .build();

        when(candidateRepository.findByUserId(candidateUserId)).thenReturn(Optional.of(candidate));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(openJob));
        when(recruiterProfileRepository.findByUserId(candidateUserId))
                .thenReturn(Optional.of(candidateRecruiterProfile));

        JobApplicationRequest request = JobApplicationRequest.builder().build();

        // Act & Assert
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> jobApplicationService.applyToJob(candidateUserId, jobId, request));
        assertThat(ex.getMessage()).contains("You cannot apply to a job posted by you or your company.");
        verify(jobApplicationRepository, never()).save(any());
    }
}
