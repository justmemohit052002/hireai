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

        Optional<Candidate> latestCandidate =
                candidateRepository.findTopByOrderByCreatedAtDesc();

        int nextSequence = 1;

        if (latestCandidate.isPresent()) {

            String lastCandidateId =
                    latestCandidate.get().getCandidateId();

            String[] parts = lastCandidateId.split("-");

            nextSequence =
                    Integer.parseInt(parts[2]) + 1;
        }

        return String.format(
                "CAN-%d-%06d",
                Year.now().getValue(),
                nextSequence
        );
    }
}