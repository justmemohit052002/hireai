package com.vionsys.hireai.candidate.mapper;

import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;

import com.vionsys.hireai.candidate.dto.CandidateProfileRequest;
import com.vionsys.hireai.candidate.dto.CandidateRequest;
import com.vionsys.hireai.candidate.dto.CandidateResponse;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.entity.Skill;

public final class CandidateMapper {

	private CandidateMapper() {
	}


	// =========================================================
	// GENERAL CANDIDATE REQUEST → ENTITY
	// =========================================================

	public static Candidate toEntity(
			CandidateRequest request) {

		if (request == null) {
			return null;
		}

		return Candidate.builder()
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.email(request.getEmail())
				.phone(request.getPhone())
				.linkedinUrl(request.getLinkedinUrl())
				.githubUrl(request.getGithubUrl())
				.portfolioUrl(request.getPortfolioUrl())
				.currentCompany(request.getCurrentCompany())
				.currentDesignation(request.getCurrentDesignation())
				.experience(request.getExperience())
				.currentCtc(request.getCurrentCtc())
				.expectedCtc(request.getExpectedCtc())
				.noticePeriod(request.getNoticePeriod())
				.location(request.getLocation())
				.skills(new HashSet<>())
				.build();
	}


	// =========================================================
	// CANDIDATE PROFILE REQUEST → ENTITY
	// =========================================================

	public static Candidate toEntity(
			CandidateProfileRequest request) {

		if (request == null) {
			return null;
		}

		return Candidate.builder()
				.linkedinUrl(request.getLinkedinUrl())
				.githubUrl(request.getGithubUrl())
				.portfolioUrl(request.getPortfolioUrl())
				.currentCompany(request.getCurrentCompany())
				.currentDesignation(request.getCurrentDesignation())
				.experience(request.getExperience())
				.currentCtc(request.getCurrentCtc())
				.expectedCtc(request.getExpectedCtc())
				.noticePeriod(request.getNoticePeriod())
				.location(request.getLocation())
				.skills(new HashSet<>())
				.build();
	}


	// =========================================================
	// ENTITY → RESPONSE
	// =========================================================

	public static CandidateResponse toResponse(
			Candidate candidate) {

		if (candidate == null) {
			return null;
		}

		UUID resumeId =
				candidate.getResume() != null
						? candidate.getResume().getId()
						: null;

		return CandidateResponse.builder()
				.id(candidate.getId())
				.candidateId(candidate.getCandidateId())
				.firstName(candidate.getFirstName())
				.lastName(candidate.getLastName())
				.email(candidate.getEmail())
				.phone(candidate.getPhone())
				.linkedinUrl(candidate.getLinkedinUrl())
				.githubUrl(candidate.getGithubUrl())
				.portfolioUrl(candidate.getPortfolioUrl())
				.currentCompany(candidate.getCurrentCompany())
				.currentDesignation(candidate.getCurrentDesignation())
				.experience(candidate.getExperience())
				.currentCtc(candidate.getCurrentCtc())
				.expectedCtc(candidate.getExpectedCtc())
				.noticePeriod(candidate.getNoticePeriod())
				.location(candidate.getLocation())
				.candidateStatus(candidate.getCandidateStatus())
				.resumeId(resumeId)
				.skillIds(
						candidate.getSkills() != null
								? candidate.getSkills()
								.stream()
								.map(Skill::getId)
								.collect(
										Collectors.toSet()
								)
								: new HashSet<>()
				)
				.createdAt(candidate.getCreatedAt())
				.updatedAt(candidate.getUpdatedAt())
				.build();
	}
}