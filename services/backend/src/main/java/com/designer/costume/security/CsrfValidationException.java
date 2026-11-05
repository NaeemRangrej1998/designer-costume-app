package com.designer.costume.security;

public class CsrfValidationException extends RuntimeException {

    public CsrfValidationException() {
        super("CSRF validation failed");
    }
}
