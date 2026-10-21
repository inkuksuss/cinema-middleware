package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.math.BigDecimal;

@Getter @Setter
@ToString
public class MovieReservationSummary {

    private Long reservationId;

    private String code;

    private BigDecimal totalPrice;

    private Integer seatCount;

    private ReservationStatus status;

    private MovieDto.Summary movieSummary;

    private ScreeningSummary screeningSummary;

    public static MovieReservationSummary from(MovieReservation movieReservation) {
        Assert.notNull(movieReservation, "movieReservation can not be null");

        MovieReservationSummary movieReservationSummary = new MovieReservationSummary();
        movieReservationSummary.reservationId = movieReservation.getId();
        movieReservationSummary.code = movieReservation.getCode();
        movieReservationSummary.totalPrice = movieReservation.getTotalPrice();
        movieReservationSummary.seatCount = movieReservation.getSeatCount();
        movieReservationSummary.status = movieReservation.getStatus();

        return movieReservationSummary;
    }
}
