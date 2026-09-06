package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter @Setter
@ToString
public class MovieReservationDetail {

    private Long reservationId;

    private String code;

    private BigDecimal totalPrice;

    private Integer seatCount;

    private ReservationStatus status;

    private LocalDateTime expiredAt;

    private LocalDateTime canceledAt;

    public static MovieReservationDetail from(MovieReservation movieReservation) {
        MovieReservationDetail movieReservationDetail = new MovieReservationDetail();
        movieReservationDetail.reservationId = movieReservation.getId();
        movieReservationDetail.code = movieReservation.getCode();
        movieReservationDetail.totalPrice = movieReservation.getTotalPrice();
        movieReservationDetail.seatCount = movieReservation.getSeatCount();
        movieReservationDetail.status = movieReservation.getStatus();
        movieReservationDetail.expiredAt = movieReservation.getExpiredAt();
        movieReservationDetail.canceledAt = movieReservation.getCanceledAt();

        return movieReservationDetail;
    }
}
