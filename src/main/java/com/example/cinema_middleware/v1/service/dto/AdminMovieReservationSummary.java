package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@ToString
public class AdminMovieReservationSummary {

    private Long reservationId;

    private String code;

    private BigDecimal totalPrice;

    private String seatNames;

    private Integer seatCount;

    private ReservationStatus status;

    private ScreeningDto.AdminSummary screeningSummary;

    public static AdminMovieReservationSummary from(MovieReservation movieReservation) {
        Assert.notNull(movieReservation, "movieReservation can not be null");

        AdminMovieReservationSummary movieReservationSummary = new AdminMovieReservationSummary();
        movieReservationSummary.reservationId = movieReservation.getId();
        movieReservationSummary.code = movieReservation.getCode();
        movieReservationSummary.totalPrice = movieReservation.getTotalPrice();
        movieReservationSummary.seatCount = movieReservation.getSeatCount();
        movieReservationSummary.status = movieReservation.getStatus();

        return movieReservationSummary;
    }
}
