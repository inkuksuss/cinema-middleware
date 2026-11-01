package com.example.cinema_middleware.v1.support.exception;

/** 판매 중이 아닌(판매 중지 등) 좌석을 예매하려 할 때 던진다. */
public class SeatNotOnSaleException extends RuntimeException {
    public SeatNotOnSaleException(String message) {
        super(message);
    }
    public SeatNotOnSaleException() {
        super();
    }
}
