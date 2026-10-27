package com.example.cinema_middleware.v1.repository.dao;

import com.example.cinema_middleware.v1.domain.entity.enums.ReservationSeatStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatStatus;

import java.math.BigDecimal;

public class SeatDao {

    public record Info(
            Long screeningId,
            ScreeningStatus screeningStatus,

            Long theaterSeatId,
            String seatRow,
            String seatColumn,
            SeatGrade grade,
            BigDecimal price,
            SeatStatus seatStatus,

            Long reservationSeatId,
            ReservationSeatStatus reservationSeatStatus
    ){
    }

    public record SeatCounter(
            Long screeningId,
            Integer totalSeatCount,
            Integer soldOutSeatCount
    ) {
    }
}
