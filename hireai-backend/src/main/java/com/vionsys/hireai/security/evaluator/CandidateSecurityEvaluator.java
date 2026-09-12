package com.vionsys.hireai.security.evaluator;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.candidate.repository.CandidateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("candidateSecurity")
@RequiredArgsConstructor
public class CandidateSecurityEvaluator {

    private final CandidateRepository candidateRepository;

    /**
     * Checks if the candidate entity is linked to the authenticated user ID.
     */
    @Transactional(readOnly = true)
    public boolean isCandidateOwner(UUID candidateId, UUID userId) {
        if (candidateId == null || userId == null) {
            return false;
        }

        return candidateRepository.existsByIdAndUserId(candidateId, userId);
    }

    /**
     * Checks if the requested user ID matches the current authenticated user ID.
     */
    public boolean isSelf(UUID targetUserId, UUID currentUserId) {
        if (targetUserId == null || currentUserId == null) {
            return false;
        }
        return targetUserId.equals(currentUserId);
    }
}
