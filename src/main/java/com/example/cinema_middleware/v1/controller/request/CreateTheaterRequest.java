package com.example.cinema_middleware.v1.controller.request;

import jakarta.validation.constraints.NotBlank;


public record CreateTheaterRequest(
        @NotBlank(message = "name은(는) 필수 입력 항목입니다.")
        String name,
        @NotBlank(message = "description은(는) 필수 입력 항목입니다.")
        String description
) {
}
