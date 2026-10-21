package com.example.cinema_middleware.v1.support.exception;

public class ScreeningHaveReservationException extends RuntimeException {
    public ScreeningHaveReservationException(String message) {
        super(message);
    }
    public ScreeningHaveReservationException() {
        super();
    }
}
