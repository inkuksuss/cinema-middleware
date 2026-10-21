package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.TheaterSeat;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.math.BigDecimal;

@Getter
@ToString
public class AdminSeatSummary {

    private Long id;

    private String seatRow;

    private String seatColumn;

    private SeatGrade grade;

    private BigDecimal price;

    public static AdminSeatSummary from(TheaterSeat theaterSeat) {
        Assert.notNull(theaterSeat, "theaterSeat can not be null");

        AdminSeatSummary seatSummary = new AdminSeatSummary();
        seatSummary.id = theaterSeat.getId();
        seatSummary.seatRow = theaterSeat.getSeatRow();
        seatSummary.seatColumn = theaterSeat.getSeatColumn();
        seatSummary.grade = theaterSeat.getGrade();
        seatSummary.price = theaterSeat.getPrice();

        return seatSummary;
    }
}
