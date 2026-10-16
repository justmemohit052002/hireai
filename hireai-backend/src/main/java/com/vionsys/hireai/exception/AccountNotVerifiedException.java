package com.vionsys.hireai.exception;

import lombok.Getter;

@Getter
public class AccountNotVerifiedException extends RuntimeException {

    private final String email;

    public AccountNotVerifiedException(String message) {
        super(message);
        this.email = null;
    }

    public AccountNotVerifiedException(String message, String email) {
        super(message);
        this.email = email;
    }
}
