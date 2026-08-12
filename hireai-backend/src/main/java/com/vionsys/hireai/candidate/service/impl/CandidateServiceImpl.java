package com.vionsys.hireai.candidate.service.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.candidate.dto.CandidateProfileRequest;
import com.vionsys.hireai.candidate.dto.CandidateRequest;
import com.vionsys.hireai.candidate.dto.CandidateResponse;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.entity.Skill;
import com.vionsys.hireai.candidate.enums.CandidateStatus;
import com.vionsys.hireai.candidate.filter.CandidateFilter;
import com.vionsys.hireai.candidate.mapper.CandidateMapper;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.candidate.repository.SkillRepository;
import com.vionsys.hireai.candidate.service.CandidateService;
import com.vionsys.hireai.candidate.specification.CandidateSpecification;
import com.vionsys.hireai.candidate.util.CandidateIdGenerator;
import com.vionsys.hireai.exception.CandidateNotFoundException;
import com.vionsys.hireai.exception.SkillNotFoundException;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final SkillRepository skillRepository;
    private final CandidateIdGenerator candidateIdGenerator;
    private final UserRepository userRepository;


    // =========================================================
    // GENERAL CANDIDATE MANAGEMENT
    // =========================================================

    @Override
    public CandidateResponse createCandidate(
            CandidateRequest request) {

        validateDuplicateCandidate(request);

        Candidate candidate =
                CandidateMapper.toEntity(request);

        candidate.setCandidateId(
                candidateIdGenerator.generateCandidateId()
        );

        candidate.setCandidateStatus(
                CandidateStatus.ACTIVE
        );

        candidate.setSkills(
                resolveSkills(request.getSkillIds())
        );

        Candidate savedCandidate =
                candidateRepository.save(candidate);

        return CandidateMapper.toResponse(
                savedCandidate
        );
    }


    @Override
    @Transactional(readOnly = true)
    public CandidateResponse getCandidateById(
            UUID candidateId) {

        Candidate candidate =
                candidateRepository.findById(candidateId)
                        .orElseThrow(() ->
                                new CandidateNotFoundException(
                                        "Candidate not found"
                                )
                        );

        return CandidateMapper.toResponse(
                candidate
        );
    }


    @Override
    @Transactional(readOnly = true)
    public Page<CandidateResponse> getAllCandidates(
            CandidateFilter filter,
            Pageable pageable) {

        Specification<Candidate> specification =
                CandidateSpecification.withFilter(filter);

        return candidateRepository
                .findAll(specification, pageable)
                .map(CandidateMapper::toResponse);
    }


    @Override
    public CandidateResponse updateCandidate(
            UUID candidateId,
            CandidateRequest request) {

        Candidate candidate =
                candidateRepository.findById(candidateId)
                        .orElseThrow(() ->
                                new CandidateNotFoundException(
                                        "Candidate not found"
                                )
                        );

        validateDuplicateCandidateForUpdate(
                candidate,
                request
        );

        updateCandidateFields(
                candidate,
                request
        );

        candidate.setSkills(
                resolveSkills(
                        request.getSkillIds()
                )
        );

        return CandidateMapper.toResponse(
                candidate
        );
    }


    @Override
    public void deleteCandidate(
            UUID candidateId) {

        Candidate candidate =
                candidateRepository.findById(candidateId)
                        .orElseThrow(() ->
                                new CandidateNotFoundException(
                                        "Candidate not found"
                                )
                        );

        candidateRepository.delete(candidate);
    }


    // =========================================================
    // AUTHENTICATED CANDIDATE PROFILE
    // =========================================================

    @Override
    public CandidateResponse createMyProfile(
            UUID userId,
            CandidateProfileRequest request) {

        /*
         * Find the authenticated User.
         */
        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        /*
         * A User can have only one Candidate profile.
         */
        if (candidateRepository.existsByUserId(userId)) {

            throw new IllegalArgumentException(
                    "Candidate profile already exists"
            );
        }

        /*
         * Create Candidate using only
         * candidate-specific profile fields.
         */
        Candidate candidate =
                CandidateMapper.toEntity(request);

        /*
         * Associate Candidate with User.
         */
        candidate.setUser(user);

        /*
         * Account information comes from User.
         */
        candidate.setFirstName(
                user.getFirstName()
        );

        candidate.setLastName(
                user.getLastName()
        );

        candidate.setEmail(
                user.getEmail()
        );

        candidate.setPhone(
                user.getPhoneNumber()
        );

        /*
         * Generate Candidate business ID.
         */
        candidate.setCandidateId(
                candidateIdGenerator.generateCandidateId()
        );

        /*
         * New Candidate profile starts as ACTIVE.
         */
        candidate.setCandidateStatus(
                CandidateStatus.ACTIVE
        );

        /*
         * Resolve requested skills.
         */
        candidate.setSkills(
                resolveSkills(
                        request.getSkillIds()
                )
        );

        /*
         * Save Candidate profile.
         */
        Candidate savedCandidate =
                candidateRepository.save(candidate);

        return CandidateMapper.toResponse(
                savedCandidate
        );
    }


    @Override
    @Transactional(readOnly = true)
    public CandidateResponse getMyProfile(
            UUID userId) {

        Candidate candidate =
                candidateRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new CandidateNotFoundException(
                                        "Candidate profile not found"
                                )
                        );

        return CandidateMapper.toResponse(
                candidate
        );
    }


    @Override
    public CandidateResponse updateMyProfile(
            UUID userId,
            CandidateProfileRequest request) {

        Candidate candidate =
                candidateRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new CandidateNotFoundException(
                                        "Candidate profile not found"
                                )
                        );

        /*
         * Update only candidate-specific profile fields.
         *
         * firstName, lastName, email and phone
         * remain owned by User.
         */
        updateProfileFields(
                candidate,
                request
        );

        /*
         * Update skills.
         */
        candidate.setSkills(
                resolveSkills(
                        request.getSkillIds()
                )
        );

        return CandidateMapper.toResponse(
                candidate
        );
    }


    // =========================================================
    // PROFILE FIELD UPDATE
    // =========================================================

    private void updateProfileFields(
            Candidate candidate,
            CandidateProfileRequest request) {

        candidate.setLinkedinUrl(
                request.getLinkedinUrl()
        );

        candidate.setGithubUrl(
                request.getGithubUrl()
        );

        candidate.setPortfolioUrl(
                request.getPortfolioUrl()
        );

        candidate.setCurrentCompany(
                request.getCurrentCompany()
        );

        candidate.setCurrentDesignation(
                request.getCurrentDesignation()
        );

        candidate.setExperience(
                request.getExperience()
        );

        candidate.setCurrentCtc(
                request.getCurrentCtc()
        );

        candidate.setExpectedCtc(
                request.getExpectedCtc()
        );

        candidate.setNoticePeriod(
                request.getNoticePeriod()
        );

        candidate.setLocation(
                request.getLocation()
        );
    }


    // =========================================================
    // GENERAL CANDIDATE VALIDATION
    // =========================================================

    private void validateDuplicateCandidate(
            CandidateRequest request) {

        if (candidateRepository.existsByEmail(
                request.getEmail())) {

            throw new IllegalArgumentException(
                    "Candidate with this email already exists"
            );
        }

        if (candidateRepository.existsByPhone(
                request.getPhone())) {

            throw new IllegalArgumentException(
                    "Candidate with this phone number already exists"
            );
        }
    }


    private void validateDuplicateCandidateForUpdate(
            Candidate candidate,
            CandidateRequest request) {

        if (!candidate.getEmail()
                .equalsIgnoreCase(
                        request.getEmail()
                )
                && candidateRepository.existsByEmail(
                request.getEmail()
        )) {

            throw new IllegalArgumentException(
                    "Candidate with this email already exists"
            );
        }

        if (!candidate.getPhone()
                .equals(
                        request.getPhone()
                )
                && candidateRepository.existsByPhone(
                request.getPhone()
        )) {

            throw new IllegalArgumentException(
                    "Candidate with this phone number already exists"
            );
        }
    }


    // =========================================================
    // SKILLS
    // =========================================================

    private Set<Skill> resolveSkills(
            Set<UUID> skillIds) {

        if (skillIds == null ||
                skillIds.isEmpty()) {

            return new HashSet<>();
        }

        Set<Skill> skills =
                new HashSet<>(
                        skillRepository.findAllById(
                                skillIds
                        )
                );

        if (skills.size() != skillIds.size()) {

            throw new SkillNotFoundException(
                    "One or more skills were not found"
            );
        }

        return skills;
    }


    // =========================================================
    // GENERAL CANDIDATE FIELD UPDATE
    // =========================================================

    private void updateCandidateFields(
            Candidate candidate,
            CandidateRequest request) {

        candidate.setFirstName(
                request.getFirstName()
        );

        candidate.setLastName(
                request.getLastName()
        );

        candidate.setEmail(
                request.getEmail()
        );

        candidate.setPhone(
                request.getPhone()
        );

        candidate.setLinkedinUrl(
                request.getLinkedinUrl()
        );

        candidate.setGithubUrl(
                request.getGithubUrl()
        );

        candidate.setPortfolioUrl(
                request.getPortfolioUrl()
        );

        candidate.setCurrentCompany(
                request.getCurrentCompany()
        );

        candidate.setCurrentDesignation(
                request.getCurrentDesignation()
        );

        candidate.setExperience(
                request.getExperience()
        );

        candidate.setCurrentCtc(
                request.getCurrentCtc()
        );

        candidate.setExpectedCtc(
                request.getExpectedCtc()
        );

        candidate.setNoticePeriod(
                request.getNoticePeriod()
        );

        candidate.setLocation(
                request.getLocation()
        );
    }
}