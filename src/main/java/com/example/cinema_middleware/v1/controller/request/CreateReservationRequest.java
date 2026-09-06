package com.example.cinema_middleware.v1.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateReservationRequest(
        @NotBlank(message = "screeningId은 필수 입력 항목입니다.")
        Long screeningId,

        @NotEmpty(message = "예약 좌석이 옳바르지 않습니다.")
        @Size(min = 1, max = 4, message = "1회 최대 좌석은 4자리 입니다.")
        List<Long> theaterSeatIdList
) {
}
