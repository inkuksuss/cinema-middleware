package com.example.cinema_middleware.v1.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 인증 API 요청 DTO 모음.
 */
public final class AuthRequest {

    private AuthRequest() {
    }

    public record SignIn(
            @Email(message = "이메일 형식이 옳바르지 않습니다.")
            @NotBlank(message = "이메일은 필수 입력 항목입니다.")
            String email,

            @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
            String password
    ) {
    }

    public record Reissue(
            @NotBlank(message = "refreshToken은 필수 입력 항목입니다.")
            String refreshToken
    ) {
    }
}
