package com.example.cinema_middleware.v1.support.exception;

public class DuplicateValueException extends RuntimeException {
    public DuplicateValueException(String message) {
        super(message);
    }
    public DuplicateValueException() {
        super();
    }
}
