package com.vionsys.hireai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vionsys.hireai.candidate.exception.CandidateNotFoundException;
import com.vionsys.hireai.candidate.exception.DuplicateResourceException;

import com.vionsys.hireai.candidate.exception.FileStorageException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	  @ExceptionHandler(CandidateNotFoundException.class)
	    public ResponseEntity<ErrorResponse> handleCandidateNotFound(
	            CandidateNotFoundException ex,
	            HttpServletRequest request) {

		  ErrorResponse response = new ErrorResponse(
	                false,
	                HttpStatus.NOT_FOUND.value(),
	                HttpStatus.NOT_FOUND.getReasonPhrase(),
	                ex.getMessage(),
	                request.getRequestURI()
	        );

	        return ResponseEntity
	                .status(HttpStatus.NOT_FOUND)
	                .body(response);
	    }

	    @ExceptionHandler(DuplicateResourceException.class)
	    public ResponseEntity<ErrorResponse> handleDuplicateResource(
	            DuplicateResourceException ex,HttpServletRequest request) {

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

	    @ExceptionHandler(FileStorageException.class)
	    public ResponseEntity<ErrorResponse> handleFileStorageException(
	            FileStorageException ex,HttpServletRequest request) {

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

	    @ExceptionHandler(Exception.class)
	    public ResponseEntity<ErrorResponse> handleException(
	            Exception ex,HttpServletRequest request) {

	    	 ErrorResponse response = new ErrorResponse(
	                 false,
	                 HttpStatus.INTERNAL_SERVER_ERROR.value(),
	                 HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
	                 "Something went wrong.",
	                 request.getRequestURI()
	         );

	         return ResponseEntity
	                 .status(HttpStatus.INTERNAL_SERVER_ERROR)
	                 .body(response);
	    }
	    
	    @ExceptionHandler(MethodArgumentNotValidException.class)
	    public ResponseEntity<ErrorResponse> handleValidationException(
	            MethodArgumentNotValidException ex, HttpServletRequest request) {

	    	String message = ex.getBindingResult()
	                .getFieldErrors()
	                .stream()
	                .findFirst()
	                .map(error ->
	                        error.getField() + " : " + error.getDefaultMessage())
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
	
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(false,HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(),
				"Invalid email or password", request.getRequestURI());

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	@ExceptionHandler(UserAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(false,HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(false,HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	@ExceptionHandler(RecruiterProfileNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleRecruiterProfileNotFound(RecruiterProfileNotFoundException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(false,HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	@ExceptionHandler(RecruiterProfileAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleRecruiterProfileAlreadyExists(RecruiterProfileAlreadyExistsException ex,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(false,HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(),
				request.getRequestURI());

		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}
	
	@ExceptionHandler(RoleNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleRoleNotFound(
	        RoleNotFoundException ex,
	        HttpServletRequest request) {

	    ErrorResponse response = new ErrorResponse(
	            false,HttpStatus.NOT_FOUND.value(),
	            "Not Found",
	            ex.getMessage(),
	            request.getRequestURI()
	    );

	    return ResponseEntity.status(HttpStatus.NOT_FOUND)
	            .body(response);
	}

}