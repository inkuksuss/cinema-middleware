package com.example.cinema_middleware.v1.support.exception;

/** 예매 요청에 같은 좌석이 두 번 이상 포함됐을 때 던진다. */
public class DuplicateSeatException extends RuntimeException {
    public DuplicateSeatException(String message) {
        super(message);
    }
    public DuplicateSeatException() {
        super();
    }
}
