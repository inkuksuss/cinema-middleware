package com.example.cinema_middleware.v1.support.exception;

public class TheaterInUseException extends RuntimeException {
    public TheaterInUseException(String message) {
        super(message);
    }
    public TheaterInUseException() {
        super();
    }
}
