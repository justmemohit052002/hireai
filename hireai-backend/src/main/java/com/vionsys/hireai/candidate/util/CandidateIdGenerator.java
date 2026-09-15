package com.vionsys.hireai.candidate.util;

import java.time.Year;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.vionsys.hireai.candidate.repository.CandidateRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CandidateIdGenerator {

    private final CandidateRepository candidateRepository;

    public synchronized String generateCandidateId() {

        int currentYear = Year.now().getValue();
        Optional<String> latestCandidateIdOpt =
                candidateRepository.findTopCandidateIdForYear(currentYear);

        int nextSequence = 1;

        if (latestCandidateIdOpt.isPresent() && latestCandidateIdOpt.get() != null) {

            String lastCandidateId =
                    latestCandidateIdOpt.get();

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

        String candidateId = String.format(
                "CAN-%d-%06d",
                currentYear,
                nextSequence
        );

        while (candidateRepository.existsByCandidateIdNative(candidateId)) {
            nextSequence++;
            candidateId = String.format(
                    "CAN-%d-%06d",
                    currentYear,
                    nextSequence
            );
        }

        return candidateId;
    }
}