package com.example.cinema_middleware.v1.support.exception;

public class ScreeningNotOnSaleException extends RuntimeException {
    public ScreeningNotOnSaleException(String message) {
        super(message);
    }
    public ScreeningNotOnSaleException() {
        super();
    }
}
