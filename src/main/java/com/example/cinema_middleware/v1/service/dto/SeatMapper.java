package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import com.example.cinema_middleware.v1.repository.dto.ScreeningSeat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@ToString
public class SeatMapper {

    private Long screeningId;

    private List<SeatData> seats = new ArrayList<>();

    public static SeatMapper from(List<ScreeningSeat> screeningSeatList) {
        SeatMapper seatMapper = new SeatMapper();
        seatMapper.screeningId = screeningSeatList.getFirst().getScreeningId();

        for (ScreeningSeat screeningSeat : screeningSeatList) {
            SeatData seatData = new SeatData();
            seatData.theaterSeatId = screeningSeat.getTheaterSeatId();
            seatData.seatRow = screeningSeat.getSeatRow();
            seatData.seatColumn = screeningSeat.getSeatColumn();
            seatData.seatGrade = screeningSeat.getSeatGrade();
            seatData.price = screeningSeat.getPrice();
            seatData.reservationSeatId = screeningSeat.getReservationSeatId();
            seatData.isActive = screeningSeat.getIsActive();

            seatMapper.seats.add(seatData);
        }
        return seatMapper;
    }

    @Getter
    @ToString
    static class SeatData {

        private Long theaterSeatId;

        private String seatRow;

        private String seatColumn;

        private SeatGrade seatGrade;

        private BigDecimal price;

        private Long reservationSeatId;

        private String isActive;
    }
}
