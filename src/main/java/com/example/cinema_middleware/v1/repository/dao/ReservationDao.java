package com.example.cinema_middleware.v1.repository.dao;

import com.example.cinema_middleware.v1.domain.entity.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public final class ReservationDao {

    public record MyReservation(
            UUID reservationId,
            ReservationStatus status,

            Long movieId,
            String movieTitle,
            String posterUrl,

            Long theaterId,
            String theaterName,

            Long screeningId,
            LocalDate screeningStartDate,
            LocalTime screeningStartAt,
            ScreeningStatus screeningStatus,
            ScreenType screenType,

            Long cinemaId,
            String cinemaName
    ) {
    }

    public record MyReservationDetail(
            UUID reservationId,
            BigDecimal originalAmount,
            String seatNames,
            ReservationStatus status,

            Long memberId,

            Long movieId,
            String movieTitle,

            Long theaterId,
            String theaterName,

            Long screeningId,
            LocalDate screeningStartDate,
            LocalTime screeningStartAt,
            ScreeningStatus screeningStatus,
            ScreenType screenType,

            Long cinemaId,
            String cinemaName,

            Long couponIssuanceId,

            BigDecimal paidAmount,
            PaymentMethod method,
            LocalDateTime paidAt,
            LocalDateTime canceledAt,
            PaymentStatus paymentStatus
    ){
    }

    public record AdminRow(
            UUID reservationId,
            ReservationStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,

            Long memberId,
            String memberName,

            Long movieId,
            String movieTitle,

            Long theaterId,
            String theaterName,

            Long screeningId,
            LocalDate screeningStartDate,
            LocalTime screeningStartAt,
            ScreeningStatus screeningStatus,
            ScreenType screenType,

            Long cinemaId,
            String cinemaName
    ){
    }

    public record AdminRowDetail(
            UUID reservationId,
            BigDecimal originalAmount,
            String seatNames,
            ReservationStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,

            Long memberId,
            String memberName,

            Long movieId,
            String movieTitle,

            Long theaterId,
            String theaterName,

            Long screeningId,
            LocalDate screeningStartDate,
            LocalTime screeningStartAt,
            ScreeningStatus screeningStatus,
            ScreenType screenType,

            Long cinemaId,
            String cinemaName,

            Long couponIssuanceId,

            BigDecimal paidAmount,
            PaymentMethod method,
            LocalDateTime paidAt,
            LocalDateTime canceledAt,
            PaymentStatus paymentStatus
    ){
    }
}
