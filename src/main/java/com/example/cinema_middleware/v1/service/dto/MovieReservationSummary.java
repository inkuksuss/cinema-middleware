package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter @Setter
@ToString
public class MovieReservationSummary {

    private Long reservationId;

    private String code;

    private BigDecimal totalPrice;

    private Integer seatCount;

    private ReservationStatus status;

    private MovieSummary movieSummary;

    private ScreeningSummary screeningSummary;

    public static MovieReservationSummary from(MovieReservation movieReservation) {
        MovieReservationSummary movieReservationSummary = new MovieReservationSummary();
        movieReservationSummary.reservationId = movieReservation.getId();
        movieReservationSummary.code = movieReservation.getCode();
        movieReservationSummary.totalPrice = movieReservation.getTotalPrice();
        movieReservationSummary.seatCount = movieReservation.getSeatCount();
        movieReservationSummary.status = movieReservation.getStatus();

        return movieReservationSummary;
    }

    public void addMovieSummary(Movie movie) {
        this.movieSummary = MovieSummary.from(movie);
    }

    public void addScreeningSummary(Screening screening) {
        this.screeningSummary = ScreeningSummary.from(screening);
    }
}
