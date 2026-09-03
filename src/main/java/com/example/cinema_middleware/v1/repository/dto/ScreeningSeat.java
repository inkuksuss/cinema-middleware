package com.example.cinema_middleware.v1.repository.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter @Setter
@ToString
public class ScreeningSeat {

    private Long theaterSeatId;

    private String seatRow;

    private String seatColumn;

    private SeatGrade seatGrade;

    private Long reservationSeatId;

    private Long screeningId;
ㅡ
    private BigDecimal
}
