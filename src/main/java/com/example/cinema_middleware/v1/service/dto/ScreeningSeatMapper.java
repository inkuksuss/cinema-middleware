package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import com.example.cinema_middleware.v1.domain.entity.enums.YesOrNo;
import com.example.cinema_middleware.v1.repository.dto.ScreeningSeat;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
public class ScreeningSeatMapper {

    private Long screeningId;

    private List<UserSeat> seatList = new ArrayList<>();

    public static ScreeningSeatMapper from(List<ScreeningSeat> screeningSeatList) {
        Assert.notNull(screeningSeatList, "screeningSeatList can not be null");

        ScreeningSeatMapper seatMapper = new ScreeningSeatMapper();
        seatMapper.screeningId = screeningSeatList.getFirst().getScreeningId();

        for (ScreeningSeat screeningSeat : screeningSeatList) {
            UserSeat userSeat = new UserSeat();
            userSeat.theaterSeatId = screeningSeat.getTheaterSeatId();
            userSeat.seatRow = screeningSeat.getSeatRow();
            userSeat.seatColumn = screeningSeat.getSeatColumn();
            userSeat.grade = screeningSeat.getSeatGrade();
            userSeat.price = screeningSeat.getPrice();
            userSeat.reservationSeatId = screeningSeat.getReservationSeatId();
            userSeat.isActive = screeningSeat.getIsActive();

            seatMapper.seatList.add(userSeat);
        }
        return seatMapper;
    }

    @Getter
    @ToString
    private static class UserSeat {

        private Long theaterSeatId;

        private String seatRow;

        private String seatColumn;

        private SeatGrade grade;

        private BigDecimal price;

        private Long reservationSeatId;

        private YesOrNo isActive;
    }
}
