package com.example.cinema_middleware.v1.controller.response;

public enum ResponseCode {

    SUCCESS(0),
    DUPLICATE_EMAIL(1),
    NO_MATCH_EMAIL_OR_PASSWORD(2),
    INVALID_REFRESH_TOKEN(3),
    //TODO

    INVALID_ARGUMENT(5),
    ILLEGAL_STATE(6),
    NO_SUPPORT_METHOD(7),
    NO_SUPPORT_CONTENT_TYPE(8),
    UNAUTHORIZED(9),
    FORBIDDEN(10),
    NOT_FOUND(11),
    PAYMENT_FAILED(12),
    EXCEPTION(99);

    private final int code;

    ResponseCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
