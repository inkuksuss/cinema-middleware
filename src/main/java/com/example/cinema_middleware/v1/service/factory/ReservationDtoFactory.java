package com.example.cinema_middleware.v1.service.factory;

import com.example.cinema_middleware.v1.repository.dao.ReservationDao;
import com.example.cinema_middleware.v1.service.dto.ReservationDto;
import org.springframework.util.Assert;

/**
 * 예매 응답 DTO 조립기.
 * 엔티티 → {@link ReservationDto} 변환의 유일한 지점이다.
 */
public final class ReservationDtoFactory {

    private ReservationDtoFactory() {
    }

    public static ReservationDto.Summary summary(ReservationDao.MyReservation reservation) {
        Assert.notNull(reservation, "reservation can not be null");

        return new ReservationDto.Summary(
                reservation.reservationId(),
                reservation.code(),
                reservation.status(),

                reservation.movieId(),
                reservation.movieTitle(),
                reservation.posterUrl(),

                reservation.theaterId(),
                reservation.theaterName(),

                reservation.screeningId(),
                reservation.screeningStartDate(),
                reservation.screeningStartAt(),
                reservation.screeningStatus(),
                reservation.screenType(),

                reservation.cinemaId(),
                reservation.cinemaName()
        );
    }

    /** 확정된 결제가 없으면(예: 결제 전·결제 실패) paidAmount 는 null 이다. */
    public static ReservationDto.Detail detail(ReservationDao.MyReservationDetail reservation) {
        Assert.notNull(reservation, "reservation can not be null");

        return new ReservationDto.Detail(
                reservation.reservationId(),
                reservation.code(),
                reservation.originalAmount(),
                reservation.seatNames(),
                reservation.status(),

                reservation.movieId(),
                reservation.movieTitle(),

                reservation.theaterId(),
                reservation.theaterName(),

                reservation.screeningId(),
                reservation.screeningStartDate(),
                reservation.screeningStartAt(),
                reservation.screeningStatus(),
                reservation.screenType(),

                reservation.cinemaId(),
                reservation.cinemaName(),

                reservation.couponIssuanceId(),

                reservation.paidAmount(),
                reservation.method(),
                reservation.paidAt(),
                reservation.canceledAt(),
                reservation.paymentStatus()
        );
    }

    public static ReservationDto.AdminSummary adminSummary(ReservationDao.AdminRow adminRow) {
        Assert.notNull(adminRow, "adminRow can not be null");

        return new ReservationDto.AdminSummary(
                adminRow.reservationId(),
                adminRow.code(),
                adminRow.status(),

                adminRow.memberId(),
                adminRow.memberName(),

                adminRow.movieId(),
                adminRow.movieTitle(),

                adminRow.theaterId(),
                adminRow.theaterName(),

                adminRow.screeningId(),
                adminRow.screeningStartDate(),
                adminRow.screeningStartAt(),
                adminRow.screeningStatus(),

                adminRow.cinemaId(),
                adminRow.cinemaName()
        );
    }

    public static ReservationDto.AdminDetail adminDetail(ReservationDao.AdminRowDetail detail) {
        Assert.notNull(detail, "detail can not be null");

        return new ReservationDto.AdminDetail(
                detail.reservationId(),
                detail.code(),
                detail.originalAmount(),
                detail.seatNames(),
                detail.status(),

                detail.memberId(),
                detail.memberName(),

                detail.movieId(),
                detail.movieTitle(),

                detail.theaterId(),
                detail.theaterName(),

                detail.screeningId(),
                detail.screeningStartDate(),
                detail.screeningStartAt(),
                detail.screeningStatus(),
                detail.screenType(),

                detail.cinemaId(),
                detail.cinemaName(),

                detail.couponIssuanceId(),

                detail.paidAmount(),
                detail.method(),
                detail.paidAt(),
                detail.canceledAt(),
                detail.paymentStatus()
        );
    }
}
