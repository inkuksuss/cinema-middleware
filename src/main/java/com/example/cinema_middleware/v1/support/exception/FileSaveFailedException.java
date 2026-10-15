package com.example.cinema_middleware.v1.support.exception;

public class FileSaveFailedException extends RuntimeException {
    public FileSaveFailedException(String message) {
        super(message);
    }
    public FileSaveFailedException() {
        super();
    }
}
