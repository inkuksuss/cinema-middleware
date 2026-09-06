package com.example.cinema_middleware.v1.support.exception;

public class ReservationExpiredException extends RuntimeException {
    public ReservationExpiredException(String message) {
        super(message);
    }
    public ReservationExpiredException() {
        super();
    }
}
