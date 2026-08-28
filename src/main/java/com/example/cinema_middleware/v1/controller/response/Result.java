package com.example.cinema_middleware.v1.controller.response;

import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;


public class Result<T> {

    private int code;

    private String message;

    private T data;

    public static <T> Result<T> of(int code, @Nullable String message, @Nullable T data) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.data = data;

        return result;
    }

    public static <T> Result<Page<T>> ofPage(int code, @Nullable String message, Page<T> data) {
        return of(code, message, data);
    }

    public static <T> Result<Slice<T>> ofSlice(int code, @Nullable String message, Slice<T> data) {
        return of(code, message, data);
    }

    public static <T> Result<T> ofSuccess(@Nullable String message, @Nullable T data) {
        return of(HttpStatus.OK.value(), message, data);
    }

    public static <T> Result<T> ofSuccess(@Nullable T data) {
        return ofSuccess(null, data);
    }

    public static <T> Result<T> ofSuccess() {
        return ofSuccess(null, null);
    }

    private Result() {};

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
