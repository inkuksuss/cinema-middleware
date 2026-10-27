package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.CinemaStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.TheaterStatus;

import java.util.List;

/**
 * 상영관 도메인 서비스 응답 DTO 모음.
 */
public final class TheaterDto {

    private TheaterDto() {
    }

    /** 상영관 요약 */
    public record Summary(
            Long id,
            String name,
            TheaterStatus status,

            Long cinemaId,
            String cinemaName,
            CinemaStatus cinemaStatus
    ) {
    }

    /** 상영관 좌석 배치 응답 */
    public record ScreenMap(
            Long screeningId,
            ScreeningStatus screeningStatus,
            List<SeatDto.Summary> seatList
    ) {
    }

    /** 영화관 좌석 배치 응답 */
    public record TheaterMap(
            Long theaterId,
            List<SeatDto.Summary> seatList
    ) {
    }
}
