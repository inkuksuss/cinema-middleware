package com.example.cinema_middleware.v1.controller.request;

import jakarta.validation.constraints.NotBlank;

public record ReissueRequest(
        @NotBlank(message = "refreshToken은 필수 입력 항목입니다.")
        String refreshToken
) {
}
