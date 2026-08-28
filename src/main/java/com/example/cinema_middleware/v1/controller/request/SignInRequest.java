package com.example.cinema_middleware.v1.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public record SignInRequest(
        @Email(message = "이메일 형식이 옳바르지 않습니다.")
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        String password
) {
}
