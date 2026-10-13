package com.example.cinema_middleware.v1.support;


import com.example.cinema_middleware.v1.controller.response.ResponseCode;
import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.support.exception.InvalidAccessTokenException;
import com.example.cinema_middleware.v1.support.exception.TargetNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionAdvice {


    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<Result<Void>> accessDeniedExceptionHandler(AccessDeniedException e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Result.of(ResponseCode.UNAUTHORIZED.getCode(), "잘못된 접근입니다.", null));
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(value = InvalidAccessTokenException.class)
    public ResponseEntity<Result<Void>> invalidAccessTokenExceptionHandler(InvalidAccessTokenException e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Result.of(ResponseCode.UNAUTHORIZED.getCode(), "잘못된 인증 토큰입니다.", null));
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(value = BadCredentialsException.class)
    public ResponseEntity<Result<Void>> badCredentialsExceptionHandler(BadCredentialsException e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Result.of(ResponseCode.NO_MATCH_EMAIL_OR_PASSWORD.getCode(), "존재하지 않는 이메일 혹은 패스워드입니다.", null));
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(value = AuthenticationException.class)
    public ResponseEntity<Result<Void>> authenticationExceptionHandler(AuthenticationException e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Result.of(ResponseCode.ILLEGAL_STATE.getCode(), "Illegal authentication", null));
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = TargetNotFoundException.class)
    public ResponseEntity<Result<Void>> targetNotFoundExceptionHandler(TargetNotFoundException e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Result.of(ResponseCode.NOT_FOUND.getCode(), "대상을 찾을 수 없습니다.", null));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> invalidArgumentExceptionHandler(MethodArgumentNotValidException e) {
        log.error("[ex handler] ex", e);
        StringBuilder defaultMsg = new StringBuilder();
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        if (allErrors.size() > 0) defaultMsg.append(allErrors.getFirst().getDefaultMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.of(ResponseCode.INVALID_ARGUMENT.getCode(), defaultMsg.toString(), null));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> illegalArgumentExceptionHandler(IllegalArgumentException e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.of(ResponseCode.INVALID_ARGUMENT.getCode(), e.getMessage(), null));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = { ConstraintViolationException.class, DataIntegrityViolationException.class })
    public ResponseEntity<Result<Void>> databaseExceptionHandler(MethodArgumentNotValidException e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.of(ResponseCode.INVALID_ARGUMENT.getCode(), "Invalid request", null));
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Result<Void>> internalServerExceptionHandler(Exception e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.of(ResponseCode.EXCEPTION.getCode(), "Server error", null));
    }
}
