package com.example.cinema_middleware.v1.repository.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import com.example.cinema_middleware.v1.domain.entity.enums.YesOrNo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@ToString
@AllArgsConstructor
public class ScreeningSeat {

    private Long screeningId;

    private Long theaterSeatId;

    private String seatRow;

    private String seatColumn;

    private SeatGrade seatGrade;

    private BigDecimal price;

    private Long reservationSeatId;

    private YesOrNo isActive;

}
