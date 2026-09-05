package com.vionsys.hireai.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.vionsys.hireai.ai.exception.AiEngineException;
import com.vionsys.hireai.candidate.exception.DuplicateResourceException;
import com.vionsys.hireai.candidate.exception.FileStorageException;
import com.vionsys.hireai.candidate.exception.ResumeNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// =========================================================
	// AUTHENTICATION & SECURITY EXCEPTIONS
	// =========================================================

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleBadCredentials(
			BadCredentialsException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.UNAUTHORIZED.value(),
				HttpStatus.UNAUTHORIZED.getReasonPhrase(),
				ex.getMessage() != null ? ex.getMessage() : "Invalid email or password",
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(response);
	}

	@ExceptionHandler(AccountLockedException.class)
	public ResponseEntity<ErrorResponse> handleAccountLocked(
			AccountLockedException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.LOCKED.value(),
				HttpStatus.LOCKED.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.LOCKED)
				.body(response);
	}

	@ExceptionHandler(TokenReuseDetectedException.class)
	public ResponseEntity<ErrorResponse> handleTokenReuseDetected(
			TokenReuseDetectedException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.UNAUTHORIZED.value(),
				HttpStatus.UNAUTHORIZED.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(response);
	}

	@ExceptionHandler(InvalidTokenException.class)
	public ResponseEntity<ErrorResponse> handleInvalidToken(
			InvalidTokenException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.UNAUTHORIZED.value(),
				HttpStatus.UNAUTHORIZED.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(response);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(
			AccessDeniedException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.FORBIDDEN.value(),
				HttpStatus.FORBIDDEN.getReasonPhrase(),
				ex.getMessage() != null ? ex.getMessage()
						: "Access Denied: You do not have permission to access this resource",
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.FORBIDDEN)
				.body(response);
	}

	// =========================================================
	// NOT FOUND EXCEPTIONS
	// =========================================================

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleUserNotFound(
			UserNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}

	@ExceptionHandler(RecruiterProfileNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleRecruiterProfileNotFound(
			RecruiterProfileNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}

	@ExceptionHandler(RoleNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleRoleNotFound(
			RoleNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}

	@ExceptionHandler(JobNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleJobNotFound(
			JobNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}

	@ExceptionHandler(ApplicationNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleApplicationNotFound(
			ApplicationNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}

	@ExceptionHandler(CandidateNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleCandidateNotFound(
			CandidateNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}

	@ExceptionHandler(SkillNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleSkillNotFound(
			SkillNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}

	@ExceptionHandler(ResumeNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleResumeNotFound(
			ResumeNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFound(
			NoResourceFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				"Requested endpoint or static resource was not found",
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(response);
	}

	// =========================================================
	// CONFLICT & DUPLICATE RESOURCE EXCEPTIONS
	// =========================================================

	@ExceptionHandler(UserAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
			UserAlreadyExistsException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(response);
	}

	@ExceptionHandler(RecruiterProfileAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleRecruiterProfileAlreadyExists(
			RecruiterProfileAlreadyExistsException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(response);
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateResource(
			DuplicateResourceException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(response);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
			DataIntegrityViolationException ex,
			HttpServletRequest request) {

		log.error("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMessage());

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				"Database constraint violation: duplicate record or unique field conflict",
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(response);
	}

	// =========================================================
	// VALIDATION & CLIENT REQUEST EXCEPTIONS
	// =========================================================

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {

		String message = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.findFirst()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.orElse("Validation failed.");

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				message,
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(response);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Malformed JSON request or invalid field format",
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(response);
	}

	@ExceptionHandler(FileStorageException.class)
	public ResponseEntity<ErrorResponse> handleFileStorageException(
			FileStorageException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(response);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(
			IllegalArgumentException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(response);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResponse> handleIllegalState(
			IllegalStateException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(response);
	}

	// =========================================================
	// THIRD PARTY / AI SERVICE EXCEPTIONS
	// =========================================================

	@ExceptionHandler(AiEngineException.class)
	public ResponseEntity<ErrorResponse> handleAiEngineException(
			AiEngineException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(
				false,
				HttpStatus.BAD_GATEWAY.value(),
				"AI Service Error",
				ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.BAD_GATEWAY)
				.body(response);
	}

	// =========================================================
	// GENERIC FALLBACK EXCEPTION
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
				request.getRequestURI());

		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(response);
	}
}