package com.example.cinema_middleware.v1.repository.dao;

import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreenType;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class ScreeningDao {

    public record Preview(
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
            Long cinemaName,

            Integer totalSeatCount,
            Integer reservedSeatCount
    ) {
    }

    public record AdminRow(
            Long screeningId,
            LocalDate startDate,
            LocalTime startAt,
            ScreeningStatus status,
            ScreenType screenType,

            Long movieId,
            String movieTitle,
            Integer movieRunningTime,
            MovieAgeRating movieAgeRating,

            Long theaterId,
            String theaterName,

            Long cinemaId,
            String cinemaName
    ) {
    }

    public record AdminRowDetail(
            Long screeningId,
            LocalDate startDate,
            LocalTime startAt,
            ScreeningStatus status,
            ScreenType screenType,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long createdBy,
            Long updatedBy,

            Long movieId,
            String movieTitle,
            Integer movieRunningTime,
            MovieAgeRating movieAgeRating,
            MovieCategory movieCategory,

            Long theaterId,
            String theaterName,

            Long cinemaId,
            String cinemaName
    ) {
    }
}
