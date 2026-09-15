package com.vionsys.hireai.candidate.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.dto.CandidateProfileRequest;
import com.vionsys.hireai.candidate.dto.CandidateRequest;
import com.vionsys.hireai.candidate.dto.CandidateResponse;
import com.vionsys.hireai.candidate.enums.CandidateStatus;
import com.vionsys.hireai.candidate.filter.CandidateFilter;
import com.vionsys.hireai.candidate.service.CandidateService;
import com.vionsys.hireai.candidate.storage.ProfilePhotoStorageService;
import com.vionsys.hireai.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Candidate Profile & Directory", description = "Endpoints for candidate self-service profile management, photo upload, and recruiter candidate directory")
public class CandidateController {

	private final CandidateService candidateService;
	private final ProfilePhotoStorageService photoStorageService;


	// =========================================================
	// AUTHENTICATED CANDIDATE PROFILE
	// =========================================================

	@Operation(summary = "Create Candidate Profile (Self-Service)", description = "Create initial candidate profile with work experience, CTC, skills, and links")
	@PostMapping("/candidate/profile")
	@PreAuthorize("hasRole('CANDIDATE')")
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


	@Operation(summary = "Get Candidate Profile (Self-Service)", description = "Fetch profile data, skills, and parsed resume info for authenticated candidate")
	@GetMapping("/candidate/profile")
	@PreAuthorize("hasRole('CANDIDATE')")
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


	@Operation(summary = "Update Candidate Profile (Self-Service)", description = "Update skills, experience, designation, CTC, or location")
	@PutMapping("/candidate/profile")
	@PreAuthorize("hasRole('CANDIDATE')")
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

	@Operation(summary = "Upload Candidate Profile Photo", description = "Upload candidate profile picture (.jpg, .jpeg, .png)")
	@PostMapping(value = "/candidate/profile/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('CANDIDATE')")
	public ResponseEntity<CandidateResponse> uploadMyProfilePhoto(
			Authentication authentication,
			@Parameter(description = "Image file (.jpg, .jpeg, .png)", required = true)
			@RequestParam("file") MultipartFile file) {

		CustomUserDetails userDetails = getAuthenticatedUser(authentication);
		CandidateResponse response = candidateService.uploadMyProfilePhoto(userDetails.getId(), file);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Get Candidate's Own Profile Photo Binary", description = "View/download the authenticated candidate's profile picture")
	@GetMapping("/candidate/profile/photo")
	@PreAuthorize("hasRole('CANDIDATE')")
	public ResponseEntity<Resource> getMyProfilePhoto(
			Authentication authentication) {

		CustomUserDetails userDetails = getAuthenticatedUser(authentication);
		Resource resource = candidateService.getMyProfilePhoto(userDetails.getId());
		String photoPath = candidateService.getMyPhotoPath(userDetails.getId());
		MediaType mediaType = photoStorageService.determineMediaType(photoPath);

		return ResponseEntity.ok()
				.contentType(mediaType)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline")
				.body(resource);
	}

	@Operation(summary = "Get Candidate Profile Photo by Candidate ID", description = "Recruiters and candidates can view a candidate's profile photo")
	@GetMapping("/candidates/{id}/profile/photo")
	@PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
	public ResponseEntity<Resource> getCandidateProfilePhoto(
			@PathVariable UUID id) {

		Resource resource = candidateService.getCandidateProfilePhoto(id);
		String photoPath = candidateService.getCandidatePhotoPath(id);
		MediaType mediaType = photoStorageService.determineMediaType(photoPath);

		return ResponseEntity.ok()
				.contentType(mediaType)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline")
				.body(resource);
	}

	@Operation(summary = "Delete Candidate Profile Photo", description = "Remove candidate's profile picture")
	@DeleteMapping("/candidate/profile/photo")
	@PreAuthorize("hasRole('CANDIDATE')")
	public ResponseEntity<CandidateResponse> deleteMyProfilePhoto(
			Authentication authentication) {

		CustomUserDetails userDetails = getAuthenticatedUser(authentication);
		CandidateResponse response = candidateService.deleteMyProfilePhoto(userDetails.getId());
		return ResponseEntity.ok(response);
	}


	// =========================================================
	// GENERAL CANDIDATE MANAGEMENT (RECRUITER / ADMIN)
	// =========================================================

	@Operation(summary = "Create Candidate in Directory (Recruiter/Admin)", description = "Manually add a new candidate to the employer talent directory")
	@PostMapping("/candidates")
	@PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
	public ResponseEntity<CandidateResponse> createCandidate(
			@Valid @RequestBody CandidateRequest request) {

		CandidateResponse response =
				candidateService.createCandidate(request);

		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(response);
	}


	@Operation(summary = "Get Candidate by ID (Recruiter/Admin)", description = "Fetch full candidate details from the talent pool")
	@GetMapping("/candidates/{candidateId}")
	@PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
	public ResponseEntity<CandidateResponse> getCandidateById(
			@PathVariable UUID candidateId) {

		CandidateResponse response =
				candidateService.getCandidateById(
						candidateId
				);

		return ResponseEntity.ok(response);
	}


	@Operation(summary = "Search & Filter Candidates (Recruiter/Admin)", description = "Paginated talent search by skill, experience, location, status, or name")
	@GetMapping("/candidates")
	@PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
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


	@Operation(summary = "Update Candidate Details (Recruiter/Admin)", description = "Update candidate information in talent directory")
	@PutMapping("/candidates/{candidateId}")
	@PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
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


	@Operation(summary = "Delete Candidate (Recruiter/Admin)", description = "Soft-delete a candidate record from the talent directory")
	@DeleteMapping("/candidates/{candidateId}")
	@PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
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