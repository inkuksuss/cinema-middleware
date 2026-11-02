package com.example.cinema_middleware.v1.support.exception;

/** 같은 예매를 동시에 변경하려 할 때(낙관적 락 충돌) 발생한다. */
public class ReservationConflictException extends RuntimeException {
    public ReservationConflictException(String message) {
        super(message);
    }
    public ReservationConflictException() {
        super();
    }
}
