package com.example.cinema_middleware.v1.repository.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@ToString
@AllArgsConstructor
public class ScreeningAdminRow {

    private Long screeningId;

    private Long movieId;

    private String movieTitle;

    private Long theaterId;

    private String theaterName;

    private LocalDate startDate;

    private LocalTime startAt;

    private ScreeningStatus status;
}
