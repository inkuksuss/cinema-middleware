package com.example.cinema_middleware.v1.support.exception;

public class SeatAlreadyTakenException extends RuntimeException {
    public SeatAlreadyTakenException(String message) {
        super(message);
    }
    public SeatAlreadyTakenException() { super(); }
}
