package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 상영 도메인 서비스 응답 DTO 모음.
 */
public final class ScreeningDto {

    private ScreeningDto() {
    }

    /** 사용자 상영 목록 — 잔여 좌석 수 포함 */
    public record Summary(
            Long id,
            LocalDate startDate,
            LocalTime startAt,
            ScreenType type,
            ScreeningStatus status,

            Long movieId,
            String movieTitle,
            String moviePosterUrl,
            Integer movieRunningTime,
            MovieAgeRating movieAgeRating,

            Long theaterId,
            String theaterName,

            Long cinemaId,

            Integer totalSeatCount,
            Integer availableSeatCount
    ) {
    }

    /** 사용자 상영 상세 */
    public record Detail(
            Long id,
            LocalDate startDate,
            LocalTime startAt,
            ScreenType type,
            ScreeningStatus status,

            Long movieId,
            String movieTitle,
            Integer movieRunningTime,
            MovieAgeRating movieAgeRating,
            String moviePosterUrl,

            Long theaterId,
            String theaterName,

            Long cinemaId,
            String cinemaName
    ) {
    }

    /** 관리자 상영 목록 */
    public record AdminSummary(
            Long id,
            LocalDate startDate,
            LocalTime startAt,
            ScreenType type,
            ScreeningStatus status,

            Long movieId,
            String movieTitle,
            Integer movieRunningTime,
            MovieAgeRating movieAgeRating,

            Long theaterId,
            String theaterName,

            Long cinemaId,
            Long cinemaName,

            Integer totalSeatCount,
            Integer availableSeatCount
    ) {
    }

    /** 관리자 상영 상세 */
    public record AdminDetail(
            Long id,
            LocalDate startDate,
            LocalTime startAt,
            ScreenType type,
            ScreeningStatus status,

            Long movieId,
            String movieTitle,
            Integer movieRunningTime,
            MovieAgeRating movieAgeRating,
            MovieCategory movieCategory,
            String moviePosterUrl,

            Long theaterId,
            String theaterName,

            Long cinemaId,
            String cinemaName
    ) {
    }
}
