package com.example.cinema_middleware.v1.support.exception;

public class IllegalReservationStateException extends RuntimeException {
    public IllegalReservationStateException(String message) {
        super(message);
    }
    public IllegalReservationStateException() {
        super();
    }
}
