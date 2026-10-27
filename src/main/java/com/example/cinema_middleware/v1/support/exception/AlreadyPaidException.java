package com.example.cinema_middleware.v1.support.exception;

public class AlreadyPaidException extends RuntimeException {
    public AlreadyPaidException(String message) {
        super(message);
    }
    public AlreadyPaidException() {
        super();
    }
}
