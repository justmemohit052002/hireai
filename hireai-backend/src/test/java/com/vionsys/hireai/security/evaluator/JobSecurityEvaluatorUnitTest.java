package com.vionsys.hireai.security.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vionsys.hireai.job.repository.JobRepository;

@ExtendWith(MockitoExtension.class)
class JobSecurityEvaluatorUnitTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobSecurityEvaluator jobSecurityEvaluator;

    @Test
    @DisplayName("shouldReturnTrue_whenUserIsOwnerOfJob")
    void shouldReturnTrue_whenUserIsOwnerOfJob() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(jobRepository.existsByIdAndRecruiterUserId(jobId, userId)).thenReturn(true);

        // Act
        boolean isOwner = jobSecurityEvaluator.isJobOwner(jobId, userId);

        // Assert
        assertThat(isOwner).isTrue();
        verify(jobRepository).existsByIdAndRecruiterUserId(jobId, userId);
    }

    @Test
    @DisplayName("shouldReturnFalse_whenUserIsNotOwnerOfJob")
    void shouldReturnFalse_whenUserIsNotOwnerOfJob() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(jobRepository.existsByIdAndRecruiterUserId(jobId, userId)).thenReturn(false);

        // Act
        boolean isOwner = jobSecurityEvaluator.isJobOwner(jobId, userId);

        // Assert
        assertThat(isOwner).isFalse();
    }

    @Test
    @DisplayName("shouldReturnFalse_whenJobIdOrUserIdIsNull")
    void shouldReturnFalse_whenJobIdOrUserIdIsNull() {
        assertThat(jobSecurityEvaluator.isJobOwner(null, UUID.randomUUID())).isFalse();
        assertThat(jobSecurityEvaluator.isJobOwner(UUID.randomUUID(), null)).isFalse();
        assertThat(jobSecurityEvaluator.isJobOwner(null, null)).isFalse();
    }
}

