package com.example.cinema_middleware.v1.support.exception;

public class SeatInUseException extends RuntimeException {
    public SeatInUseException(String message) {
        super(message);
    }
    public SeatInUseException() {
        super();
    }
}
