package com.vionsys.hireai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vionsys.hireai.candidate.exception.DuplicateResourceException;
import com.vionsys.hireai.candidate.exception.FileStorageException;

import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// =========================================================
	// DUPLICATE RESOURCE
	// =========================================================

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateResource(
			DuplicateResourceException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(response);
	}


	// =========================================================
	// FILE STORAGE
	// =========================================================

	@ExceptionHandler(FileStorageException.class)
	public ResponseEntity<ErrorResponse> handleFileStorageException(
			FileStorageException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(response);
	}


	// =========================================================
	// VALIDATION & JSON PARSING
	// =========================================================

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {

		String message = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.findFirst()
				.map(error ->
						error.getField()
								+ " : "
								+ error.getDefaultMessage())
				.orElse("Validation failed.");

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				message,
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(response);
	}

	@ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
			org.springframework.http.converter.HttpMessageNotReadableException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Malformed JSON request or invalid field format",
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(response);
	}

	@ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
			org.springframework.dao.DataIntegrityViolationException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				"Database constraint violation: duplicate record or unique field conflict",
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(response);
	}

	@ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFound(
			org.springframework.web.servlet.resource.NoResourceFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				"Requested resource was not found",
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}


	// =========================================================
	// BAD CREDENTIALS
	// =========================================================

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleBadCredentials(
			BadCredentialsException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.UNAUTHORIZED.value(),
				HttpStatus.UNAUTHORIZED.getReasonPhrase(),
				"Invalid email or password",
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(response);
	}


	// =========================================================
	// USER ALREADY EXISTS
	// =========================================================

	@ExceptionHandler(UserAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
			UserAlreadyExistsException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.CONFLICT.value(),
				"Conflict",
				ex.getMessage(),
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(response);
	}


	// =========================================================
	// USER NOT FOUND
	// =========================================================

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleUserNotFound(
			UserNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				"Not Found",
				ex.getMessage(),
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}


	// =========================================================
	// RECRUITER PROFILE NOT FOUND
	// =========================================================

	@ExceptionHandler(RecruiterProfileNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleRecruiterProfileNotFound(
			RecruiterProfileNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				"Not Found",
				ex.getMessage(),
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}


	// =========================================================
	// RECRUITER PROFILE ALREADY EXISTS
	// =========================================================

	@ExceptionHandler(RecruiterProfileAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleRecruiterProfileAlreadyExists(
			RecruiterProfileAlreadyExistsException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.CONFLICT.value(),
				"Conflict",
				ex.getMessage(),
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(response);
	}


	// =========================================================
	// ROLE NOT FOUND
	// =========================================================

	@ExceptionHandler(RoleNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleRoleNotFound(
			RoleNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				"Not Found",
				ex.getMessage(),
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}


	// =========================================================
	// JOB NOT FOUND
	// =========================================================

	@ExceptionHandler(JobNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleJobNotFound(
			JobNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				"Not Found",
				ex.getMessage(),
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}


	// =========================================================
	// APPLICATION NOT FOUND
	// =========================================================

	@ExceptionHandler(ApplicationNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleApplicationNotFound(
			ApplicationNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				"Not Found",
				ex.getMessage(),
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}


	// =========================================================
	// CANDIDATE NOT FOUND
	// =========================================================

	@ExceptionHandler(CandidateNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleCandidateNotFound(
			CandidateNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				"Not Found",
				ex.getMessage(),
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}


	// =========================================================
	// SKILL NOT FOUND
	// =========================================================

	@ExceptionHandler(SkillNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleSkillNotFound(
			SkillNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				"Not Found",
				ex.getMessage(),
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}


	// =========================================================
	// ACCESS DENIED
	// =========================================================

	@ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(
			org.springframework.security.access.AccessDeniedException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.FORBIDDEN.value(),
				HttpStatus.FORBIDDEN.getReasonPhrase(),
				"Access denied: You do not have permission to access this resource",
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.FORBIDDEN)
				.body(response);
	}


	// =========================================================
	// ILLEGAL ARGUMENT
	// =========================================================

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(
			IllegalArgumentException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(response);
	}


	// =========================================================
	// ILLEGAL STATE
	// =========================================================

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResponse> handleIllegalState(
			IllegalStateException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(response);
	}


	// =========================================================
	// GENERIC EXCEPTION
	// =========================================================

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(
			Exception ex,
			HttpServletRequest request) {

		log.error("Unhandled exception processing request {}: ", request.getRequestURI(), ex);

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
				ex.getMessage() != null ? ex.getMessage() : "Something went wrong.",
				request.getRequestURI()
		);

		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(response);
	}
}