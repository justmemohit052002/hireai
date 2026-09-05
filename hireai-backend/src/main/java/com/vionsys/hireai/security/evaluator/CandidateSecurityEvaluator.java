package com.vionsys.hireai.security.evaluator;

import java.util.UUID;

import org.springframework.stereotype.Component;

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
    public boolean isCandidateOwner(UUID candidateId, UUID userId) {
        if (candidateId == null || userId == null) {
            return false;
        }

        return candidateRepository.findById(candidateId)
                .map(candidate -> candidate.getUser() != null && userId.equals(candidate.getUser().getId()))
                .orElse(false);
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
