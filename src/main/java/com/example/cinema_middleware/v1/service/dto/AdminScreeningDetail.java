package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter
@ToString
public class AdminScreeningDetail {

    private Long screeningId;

    private MovieDto.Summary movieSummary;

    private TheaterDto.Summary theaterSummary;

    private LocalDate startDate;

    private LocalTime startAt;

    private ScreeningStatus status;

    public static AdminScreeningDetail from(Screening screening) {
        Assert.notNull(screening, "screening can not be null");

        AdminScreeningDetail screeningSummary = new AdminScreeningDetail();
        screeningSummary.screeningId = screening.getId();
        screeningSummary.startDate = screening.getStartDate();
        screeningSummary.startAt = screening.getStartAt();
        screeningSummary.status = screening.getStatus();

        return screeningSummary;
    }
}
