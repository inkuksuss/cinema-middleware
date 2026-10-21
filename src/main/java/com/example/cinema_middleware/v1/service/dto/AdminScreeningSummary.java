package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.repository.dto.ScreeningAdminRow;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter
@ToString
public class AdminScreeningSummary {

    private Long screeningId;

    private MovieSummary movieSummary;

    private TheaterSummary theaterSummary;

    private LocalDate startDate;

    private LocalTime startAt;

    private ScreeningStatus status;

    public static AdminScreeningSummary from(ScreeningAdminRow row) {
        Assert.notNull(row, "row can not be null");

        AdminScreeningSummary screeningSummary = new AdminScreeningSummary();
        screeningSummary.screeningId = row.getScreeningId();

        screeningSummary.movieSummary = MovieSummary.of(row.getMovieId(), row.getMovieTitle());
        screeningSummary.theaterSummary = TheaterSummary.of(row.getTheaterId(), row.getTheaterName());

        screeningSummary.startDate = row.getStartDate();
        screeningSummary.startAt = row.getStartAt();
        screeningSummary.status = row.getStatus();

        return screeningSummary;
    }

    public static AdminScreeningSummary from(Screening screening) {
        Assert.notNull(screening, "screening can not be null");

        AdminScreeningSummary screeningSummary = new AdminScreeningSummary();
        screeningSummary.screeningId = screening.getId();
        screeningSummary.startDate = screening.getStartDate();
        screeningSummary.startAt = screening.getStartAt();
        screeningSummary.status = screening.getStatus();

        return screeningSummary;
    }
}
