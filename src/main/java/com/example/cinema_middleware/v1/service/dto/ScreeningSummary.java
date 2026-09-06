package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.repository.dto.SeatCount;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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

    private ScreeningStatus status;

    private Integer totalSeatCount = 0;

    private Integer soldOutSeatCount = 0;

    public static ScreeningSummary from(Screening screening) {
        ScreeningSummary screeningSummary = new ScreeningSummary();
        screeningSummary.id = screening.getId();
        screeningSummary.startDate = screening.getStartDate();
        screeningSummary.startAt = screening.getStartAt();
        screeningSummary.status = screening.getStatus();

        return screeningSummary;
    }

    public void addSeatCount(List<SeatCount> seatCountList) {
        for (SeatCount seatCount : seatCountList) {
            if (this.id.equals(seatCount.getScreeningId())) {
                this.totalSeatCount = seatCount.getTotalSeatCount().intValue();
                this.soldOutSeatCount = seatCount.getSoldOutSeatCount().intValue();
            }
        }
    }
}
