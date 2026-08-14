package com.vionsys.hireai.candidate.util;

import java.time.Year;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.repository.CandidateRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CandidateIdGenerator {

    private final CandidateRepository candidateRepository;

    public String generateCandidateId() {

        int currentYear = Year.now().getValue();
        Optional<Candidate> latestCandidate =
                candidateRepository.findTopByOrderByCreatedAtDesc();

        int nextSequence = 1;

        if (latestCandidate.isPresent() && latestCandidate.get().getCandidateId() != null) {

            String lastCandidateId =
                    latestCandidate.get().getCandidateId();

            String[] parts = lastCandidateId.split("-");

            if (parts.length == 3) {
                try {
                    int lastYear = Integer.parseInt(parts[1]);
                    if (lastYear == currentYear) {
                        nextSequence = Integer.parseInt(parts[2]) + 1;
                    }
                } catch (NumberFormatException ignored) {
                    // Fall back to sequence 1 if last ID sequence cannot be parsed
                }
            }
        }

        return String.format(
                "CAN-%d-%06d",
                currentYear,
                nextSequence
        );
    }
}