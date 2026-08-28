package com.example.cinema_middleware.v1.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record SignUpRequest(
        @Email(message = "이메일 형식이 옳바르지 않습니다.")
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        String email,

        @NotBlank(message = "이름은 필수 입력 항목입니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        String password,

        @Pattern(regexp = "^010\\d{8}$", message = "전화번호 형식이 옳바르지 않습니다.")
        @NotBlank(message = "전화번호는 필수 입력 항목입니다.")
        String phoneNumber,

        @Pattern(regexp = "^(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])$", message = "생년월일 형식이 옳바르지 않습니다.")
        @NotBlank(message = "생년월일은 필수 입력 항목입니다.")
        String birthday
) {
}
