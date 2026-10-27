package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상영 좌석 도메인 서비스 응답 DTO 모음.
 */
public final class SeatDto {

    private SeatDto() {
    }

    public record Summary(
            Long theaterSeatId,
            String seatNumber,
            SeatGrade grade,
            BigDecimal price,
            SeatStatus seatStatus,

            Long reservationSeatId,
            ReservationSeatStatus reservationSeatStatus
    ) {
    }
}
