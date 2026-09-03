package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.repository.dto.SeatCount;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter @Setter
@ToString
public class ScreeningSummary {

    private Long id;

    private MovieSummary movieSummary;

    private TheaterSummary theaterSummary;

    private LocalDate startDate;

    private LocalTime startAt;

    private BigDecimal price;

    private ScreeningStatus status;

    private Integer totalSeatCount = 0;

    private Integer soldOutSeatCount = 0;

    public void mapSeatCount(List<SeatCount> seatCountList) {
        seatCountList.stream()
                .filter(count -> this.id.equals(count.getScreeningId()))
                .forEach(count -> {
                    this.totalSeatCount = count.getTotalSeatCount().intValue();
                    this.soldOutSeatCount = count.getSoldOutSeatCount().intValue();
                });
    }

    public static ScreeningSummary from(Screening screening) {
        ScreeningSummary screeningSummary = new ScreeningSummary();
        screeningSummary.id = screening.getId();
        screeningSummary.startDate = screening.getStartDate();
        screeningSummary.startAt = screening.getStartAt();
        screeningSummary.price = screening.getPrice();
        screeningSummary.status = screening.getStatus();

        if (screening.getMovie() != null) {
            screeningSummary.movieSummary = MovieSummary.from(screening.getMovie());
        }
        if (screening.getTheater() != null) {
            screeningSummary.theaterSummary = TheaterSummary.from(screening.getTheater());
        }

        return screeningSummary;
    }
}
