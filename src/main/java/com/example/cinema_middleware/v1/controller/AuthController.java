package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.request.ReissueRequest;
import com.example.cinema_middleware.v1.controller.request.SignInRequest;
import com.example.cinema_middleware.v1.controller.response.ResponseCode;
import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.service.AuthService;
import com.example.cinema_middleware.v1.service.dto.IssueTokenDto;
import com.example.cinema_middleware.v1.support.exception.InvalidAccessTokenException;
import com.example.cinema_middleware.v1.support.exception.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-in")
    public ResponseEntity<Result<IssueTokenDto>> signIn(@RequestBody @Validated SignInRequest request) {
        IssueTokenDto issueTokenDto = authService.login(request.email(), request.password());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Result.ofSuccess(issueTokenDto));
    }

    @PostMapping("/reissue")
    public ResponseEntity<Result<IssueTokenDto>> reissue(@RequestBody @Validated ReissueRequest request) {
        try {
            IssueTokenDto issueTokenDto = authService.reissue(request.refreshToken());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.ofSuccess(issueTokenDto));
        }
        catch (InvalidRefreshTokenException e) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.of(ResponseCode.INVALID_REFRESH_TOKEN.getCode(), e.getMessage(), null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Result<Void>> logout(@RequestHeader("Authorization") String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException("인증 토큰이 존재하지 않습니다.");
        }

        try {
            authService.logout(accessToken);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.ofSuccess());
        }
        catch (InvalidAccessTokenException e) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.of(ResponseCode.INVALID_ARGUMENT.getCode(), e.getMessage(), null));
        }
    }
}
