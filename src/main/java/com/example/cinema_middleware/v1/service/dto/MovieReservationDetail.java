package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.MoviePayment;
import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
@ToString
public class MovieReservationDetail {

    private Long reservationId;

    private String code;

    private BigDecimal totalPrice;

    private String seatNames;

    private Integer seatCount;

    private ReservationStatus status;

    private LocalDateTime expiredAt;

    private LocalDateTime canceledAt;

    private PaymentSummary paymentSummary;

    public static MovieReservationDetail from(MovieReservation movieReservation) {
        MovieReservationDetail movieReservationDetail = new MovieReservationDetail();
        movieReservationDetail.reservationId = movieReservation.getId();
        movieReservationDetail.code = movieReservation.getCode();
        movieReservationDetail.totalPrice = movieReservation.getTotalPrice();
        movieReservationDetail.seatNames = movieReservation.getSeatNames();
        movieReservationDetail.seatCount = movieReservation.getSeatCount();
        movieReservationDetail.status = movieReservation.getStatus();
        movieReservationDetail.expiredAt = movieReservation.getExpiredAt();
        movieReservationDetail.canceledAt = movieReservation.getCanceledAt();

        return movieReservationDetail;
    }

    public void addPaymentSummary(MoviePayment moviePayment) {
        this.paymentSummary = PaymentSummary.from(moviePayment);
    }
}
