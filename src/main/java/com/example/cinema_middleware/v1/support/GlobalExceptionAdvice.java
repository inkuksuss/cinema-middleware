package com.example.cinema_middleware.v1.support;


import com.example.cinema_middleware.v1.controller.response.ResponseCode;
import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.support.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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
                .body(Result.of(ResponseCode.ILLEGAL_STATE.getCode(), "인증에 실패하였습니다.", null));
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = TargetNotFoundException.class)
    public ResponseEntity<Result<Void>> targetNotFoundExceptionHandler(TargetNotFoundException e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Result.of(ResponseCode.NOT_FOUND.getCode(), "대상을 찾을 수 없습니다.", null));
    }

    /**
     * 매핑되지 않은 경로 요청. 클라이언트 오류이고 스캐너 요청이 잦으므로 스택 없이 WARN 한 줄만 남긴다.
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> noResourceFoundExceptionHandler(NoResourceFoundException e) {
        log.warn("[ex handler] no resource: {} /{}", e.getHttpMethod(), e.getResourcePath());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Result.of(ResponseCode.NOT_FOUND.getCode(), "존재하지 않는 경로입니다.", null));
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(value = DuplicateValueException.class)
    public ResponseEntity<Result<Void>> duplicateValueExceptionHandler(DuplicateValueException e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Result.of(ResponseCode.DUPLICATE_VALUE.getCode(), e.getMessage(), null));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = ObjectNotFoundException.class)
    public ResponseEntity<Result<Void>> objectNotFoundExceptionHandler(ObjectNotFoundException e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.of(ResponseCode.NOT_FOUND.getCode(), e.getMessage(), null));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = FileSaveFailedException.class)
    public ResponseEntity<Result<Void>> fileSaveFailedExceptionHandler(FileSaveFailedException e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.of(ResponseCode.INVALID_FILE.getCode(), e.getMessage(), null));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> invalidArgumentExceptionHandler(MethodArgumentNotValidException e) {
        log.error("[ex handler] ex", e);
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.of(ResponseCode.INVALID_ARGUMENT.getCode(), allErrors.size() > 0
                        ? allErrors.getFirst().getDefaultMessage() :
                        "잘못된 요청 파라미터입니다.", null));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.of(ResponseCode.INVALID_ARGUMENT.getCode(), "잘못된 요청 파라미터입니다.", null));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.of(ResponseCode.INVALID_ARGUMENT.getCode(), "잘못된 요청 파라미터입니다.", null));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> illegalArgumentExceptionHandler(IllegalArgumentException e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.of(ResponseCode.INVALID_ARGUMENT.getCode(), e.getMessage(), null));
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(value = { ConstraintViolationException.class, DataIntegrityViolationException.class })
    public ResponseEntity<Result<Void>> databaseExceptionHandler(Exception e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Result.of(ResponseCode.TRANSACTION_FAILED.getCode(), "Transaction failed", null));
    }

    /**
     * 혼잡·일시 장애로 정해진 시간 안에 처리하지 못한 경우. 오래 기다리지 않고 재시도를 안내한다.
     * - CannotCreateTransactionException: 커넥션 풀 대기(hikari.connection-timeout) 초과
     * - QueryTimeoutException, TransactionTimedOutException: 쿼리·트랜잭션·Redis 명령 시간 초과
     * - DataAccessResourceFailureException: DB·Redis 연결 실패(RedisConnectionFailureException 포함)
     * - PessimisticLockingFailureException: 행 잠금 대기 초과·교착
     */
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(value = {
            CannotCreateTransactionException.class,
            QueryTimeoutException.class,
            TransactionTimedOutException.class,
            DataAccessResourceFailureException.class,
            PessimisticLockingFailureException.class
    })
    public ResponseEntity<Result<Void>> serviceUnavailableExceptionHandler(Exception e) {
        log.error("[ex handler] ex", e);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.of(ResponseCode.SERVICE_UNAVAILABLE.getCode(), "잠시 후 다시 시도해주세요.", null));
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
