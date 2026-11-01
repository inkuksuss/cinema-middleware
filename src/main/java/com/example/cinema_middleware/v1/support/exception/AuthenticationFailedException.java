package com.example.cinema_middleware.v1.support.exception;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
    public AuthenticationFailedException() {
        super();
    }
}
