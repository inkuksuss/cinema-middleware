package com.example.cinema_middleware.v1.repository.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@ToString
@AllArgsConstructor
public class SeatPrice {

    private Long screeningId;

    private ScreeningStatus screeningStatus;

    private Long theaterSeatId;

    private SeatGrade grade;

    private BigDecimal price;
}
