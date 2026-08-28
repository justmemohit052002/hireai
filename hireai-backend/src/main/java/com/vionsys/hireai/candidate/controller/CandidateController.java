package com.vionsys.hireai.candidate.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vionsys.hireai.candidate.dto.CandidateProfileRequest;
import com.vionsys.hireai.candidate.dto.CandidateRequest;
import com.vionsys.hireai.candidate.dto.CandidateResponse;
import com.vionsys.hireai.candidate.enums.CandidateStatus;
import com.vionsys.hireai.candidate.filter.CandidateFilter;
import com.vionsys.hireai.candidate.service.CandidateService;
import com.vionsys.hireai.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CandidateController {

	private final CandidateService candidateService;
	private final com.vionsys.hireai.candidate.storage.ProfilePhotoStorageService photoStorageService;


	// =========================================================
	// AUTHENTICATED CANDIDATE PROFILE
	// =========================================================

	@PostMapping("/candidate/profile")
	public ResponseEntity<CandidateResponse> createMyProfile(
			Authentication authentication,
			@Valid @RequestBody CandidateProfileRequest request) {

		CustomUserDetails userDetails =
				getAuthenticatedUser(authentication);

		CandidateResponse response =
				candidateService.createMyProfile(
						userDetails.getId(),
						request
				);

		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(response);
	}


	@GetMapping("/candidate/profile")
	public ResponseEntity<CandidateResponse> getMyProfile(
			Authentication authentication) {

		CustomUserDetails userDetails =
				getAuthenticatedUser(authentication);

		CandidateResponse response =
				candidateService.getMyProfile(
						userDetails.getId()
				);

		return ResponseEntity.ok(response);
	}


	@PutMapping("/candidate/profile")
	public ResponseEntity<CandidateResponse> updateMyProfile(
			Authentication authentication,
			@Valid @RequestBody CandidateProfileRequest request) {

		CustomUserDetails userDetails =
				getAuthenticatedUser(authentication);

		CandidateResponse response =
				candidateService.updateMyProfile(
						userDetails.getId(),
						request
				);

		return ResponseEntity.ok(response);
	}

	@PostMapping(value = "/candidate/profile/photo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CandidateResponse> uploadMyProfilePhoto(
			Authentication authentication,
			@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

		CustomUserDetails userDetails = getAuthenticatedUser(authentication);
		CandidateResponse response = candidateService.uploadMyProfilePhoto(userDetails.getId(), file);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/candidate/profile/photo")
	public ResponseEntity<org.springframework.core.io.Resource> getMyProfilePhoto(
			Authentication authentication) {

		CustomUserDetails userDetails = getAuthenticatedUser(authentication);
		org.springframework.core.io.Resource resource = candidateService.getMyProfilePhoto(userDetails.getId());
		String photoPath = candidateService.getMyPhotoPath(userDetails.getId());
		org.springframework.http.MediaType mediaType = photoStorageService.determineMediaType(photoPath);

		return ResponseEntity.ok()
				.contentType(mediaType)
				.header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline")
				.body(resource);
	}

	@GetMapping("/candidates/{id}/profile/photo")
	public ResponseEntity<org.springframework.core.io.Resource> getCandidateProfilePhoto(
			@PathVariable UUID id) {

		org.springframework.core.io.Resource resource = candidateService.getCandidateProfilePhoto(id);
		String photoPath = candidateService.getCandidatePhotoPath(id);
		org.springframework.http.MediaType mediaType = photoStorageService.determineMediaType(photoPath);

		return ResponseEntity.ok()
				.contentType(mediaType)
				.header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline")
				.body(resource);
	}

	@DeleteMapping("/candidate/profile/photo")
	public ResponseEntity<CandidateResponse> deleteMyProfilePhoto(
			Authentication authentication) {

		CustomUserDetails userDetails = getAuthenticatedUser(authentication);
		CandidateResponse response = candidateService.deleteMyProfilePhoto(userDetails.getId());
		return ResponseEntity.ok(response);
	}


	// =========================================================
	// GENERAL CANDIDATE MANAGEMENT
	// =========================================================

	@PostMapping("/candidates")
	public ResponseEntity<CandidateResponse> createCandidate(
			@Valid @RequestBody CandidateRequest request) {

		CandidateResponse response =
				candidateService.createCandidate(request);

		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(response);
	}


	@GetMapping("/candidates/{candidateId}")
	public ResponseEntity<CandidateResponse> getCandidateById(
			@PathVariable UUID candidateId) {

		CandidateResponse response =
				candidateService.getCandidateById(
						candidateId
				);

		return ResponseEntity.ok(response);
	}


	@GetMapping("/candidates")
	public ResponseEntity<Page<CandidateResponse>> getAllCandidates(

			@RequestParam(required = false)
			String candidateId,

			@RequestParam(required = false)
			String firstName,

			@RequestParam(required = false)
			String lastName,

			@RequestParam(required = false)
			String email,

			@RequestParam(required = false)
			String phone,

			@RequestParam(required = false)
			String location,

			@RequestParam(required = false)
			CandidateStatus candidateStatus,

			@RequestParam(required = false)
			BigDecimal experience,

			@RequestParam(required = false)
			String skill,

			@RequestParam(defaultValue = "0")
			int page,

			@RequestParam(defaultValue = "10")
			int size,

			@RequestParam(defaultValue = "createdAt")
			String sortBy,

			@RequestParam(defaultValue = "desc")
			String direction) {

		Sort.Direction sortDirection =
				direction.equalsIgnoreCase("asc")
						? Sort.Direction.ASC
						: Sort.Direction.DESC;

		PageRequest pageable =
				PageRequest.of(
						page,
						size,
						Sort.by(
								sortDirection,
								sortBy
						)
				);

		CandidateFilter filter =
				CandidateFilter.builder()
						.candidateId(candidateId)
						.firstName(firstName)
						.lastName(lastName)
						.email(email)
						.phone(phone)
						.location(location)
						.candidateStatus(candidateStatus)
						.experience(experience)
						.skill(skill)
						.build();

		Page<CandidateResponse> response =
				candidateService.getAllCandidates(
						filter,
						pageable
				);

		return ResponseEntity.ok(response);
	}


	@PutMapping("/candidates/{candidateId}")
	public ResponseEntity<CandidateResponse> updateCandidate(
			@PathVariable UUID candidateId,
			@Valid @RequestBody CandidateRequest request) {

		CandidateResponse response =
				candidateService.updateCandidate(
						candidateId,
						request
				);

		return ResponseEntity.ok(response);
	}


	@DeleteMapping("/candidates/{candidateId}")
	public ResponseEntity<Void> deleteCandidate(
			@PathVariable UUID candidateId) {

		candidateService.deleteCandidate(
				candidateId
		);

		return ResponseEntity
				.noContent()
				.build();
	}


	// =========================================================
	// AUTHENTICATION HELPER
	// =========================================================

	private CustomUserDetails getAuthenticatedUser(
			Authentication authentication) {

		if (authentication == null ||
				!(authentication.getPrincipal()
						instanceof CustomUserDetails)) {

			throw new IllegalStateException(
					"Authenticated user not found"
			);
		}

		return (CustomUserDetails)
				authentication.getPrincipal();
	}
}