package com.example.cinema_middleware.v1.support.exception;

public class PaymentAmountMismatchException extends RuntimeException {
    public PaymentAmountMismatchException(String message) {
        super(message);
    }
    public PaymentAmountMismatchException() {
        super();
    }
}
