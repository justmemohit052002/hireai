package com.vionsys.hireai.recruiter.service.impl;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.exception.RecruiterProfileAlreadyExistsException;
import com.vionsys.hireai.exception.RecruiterProfileNotFoundException;
import com.vionsys.hireai.exception.UserNotFoundException;
import com.vionsys.hireai.recruiter.dto.RecruiterProfileRequest;
import com.vionsys.hireai.recruiter.dto.RecruiterProfileResponse;
import com.vionsys.hireai.recruiter.entity.RecruiterProfile;
import com.vionsys.hireai.recruiter.mapper.RecruiterProfileMapper;
import com.vionsys.hireai.recruiter.repository.RecruiterProfileRepository;
import com.vionsys.hireai.recruiter.service.RecruiterProfileService;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RecruiterProfileServiceImpl implements RecruiterProfileService {

	private final RecruiterProfileRepository recruiterProfileRepository;
	private final UserRepository userRepository;

	@Override
	public RecruiterProfileResponse createRecruiterProfile(RecruiterProfileRequest request) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

		User user = userRepository.findByEmail(userDetails.getUsername())
				.orElseThrow(() -> new UserNotFoundException("User not found"));

		if (recruiterProfileRepository.existsByUserId(user.getId())) {
			throw new RecruiterProfileAlreadyExistsException("Recruiter profile already exists");
		}

		RecruiterProfile recruiterProfile = RecruiterProfileMapper.toEntity(request);

		recruiterProfile.setUser(user);

		RecruiterProfile savedProfile = recruiterProfileRepository.save(recruiterProfile);

		return RecruiterProfileMapper.toResponse(savedProfile);
	}

	@Override
	@Transactional(readOnly = true)
	public RecruiterProfileResponse getCurrentRecruiterProfile() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

		User user = userRepository.findByEmail(userDetails.getUsername())
				.orElseThrow(() -> new UserNotFoundException("User not found"));

		RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(user.getId())
				.orElseThrow(() -> new RecruiterProfileNotFoundException("Recruiter profile not found"));

		return RecruiterProfileMapper.toResponse(recruiterProfile);
	}

	@Override
	@Transactional(readOnly = true)
	public RecruiterProfileResponse getRecruiterProfileByUserId(UUID userId) {

		RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(userId)
				.orElseThrow(() -> new RecruiterProfileNotFoundException("Recruiter profile not found"));

		return RecruiterProfileMapper.toResponse(recruiterProfile);
	}

	@Override
	public RecruiterProfileResponse updateRecruiterProfile(RecruiterProfileRequest request) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

		User user = userRepository.findByEmail(userDetails.getUsername())
				.orElseThrow(() -> new UserNotFoundException("User not found"));

		RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(user.getId())
				.orElseThrow(() -> new RecruiterProfileNotFoundException("Recruiter profile not found"));

		recruiterProfile.setCompanyName(request.getCompanyName());
		recruiterProfile.setDesignation(request.getDesignation());
		recruiterProfile.setCompanyWebsite(request.getCompanyWebsite());
		recruiterProfile.setCompanyEmail(request.getCompanyEmail());
		recruiterProfile.setCompanyPhone(request.getCompanyPhone());
		recruiterProfile.setCompanyLogoUrl(request.getCompanyLogoUrl());
		recruiterProfile.setCompanyDescription(request.getCompanyDescription());
		recruiterProfile.setIndustry(request.getIndustry());
		recruiterProfile.setCompanySize(request.getCompanySize());
		recruiterProfile.setCountry(request.getCountry());
		recruiterProfile.setState(request.getState());
		recruiterProfile.setCity(request.getCity());
		recruiterProfile.setAddress(request.getAddress());

		RecruiterProfile updatedProfile = recruiterProfileRepository.save(recruiterProfile);

		return RecruiterProfileMapper.toResponse(updatedProfile);
	}

}