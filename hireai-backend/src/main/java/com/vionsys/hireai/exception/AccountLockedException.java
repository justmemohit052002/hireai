package com.vionsys.hireai.exception;

import lombok.Getter;

@Getter
public class AccountLockedException extends RuntimeException {

    private final long retryAfterSeconds;

    public AccountLockedException(String message) {
        this(message, 0L);
    }

    public AccountLockedException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
