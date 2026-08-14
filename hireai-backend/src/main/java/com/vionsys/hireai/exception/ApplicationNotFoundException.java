package com.vionsys.hireai.exception;

import org.springframework.http.HttpStatus;

public class ApplicationNotFoundException extends RuntimeException {

    private final HttpStatus status = HttpStatus.NOT_FOUND;

    public ApplicationNotFoundException(String message) {
        super(message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
