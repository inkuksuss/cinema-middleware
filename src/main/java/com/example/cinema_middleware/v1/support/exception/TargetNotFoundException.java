package com.example.cinema_middleware.v1.support.exception;

public class TargetNotFoundException extends RuntimeException {
    public TargetNotFoundException(String message) {
        super(message);
    }
    public TargetNotFoundException() {
        super();
    }
}
