package com.vionsys.hireai.exception;

import org.springframework.http.HttpStatus;

public class CandidateNotFoundException extends RuntimeException {

    private final HttpStatus status = HttpStatus.NOT_FOUND;

    public CandidateNotFoundException(String message) {
        super(message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}