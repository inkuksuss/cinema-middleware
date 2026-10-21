package com.example.cinema_middleware.v1.service.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.util.List;

@Getter @Setter
@ToString
public class TheaterSeatMapper {

    private Long theaterId;

    private List<AdminSeatSummary> seatList;

    public TheaterSeatMapper(Long theaterId, List<AdminSeatSummary> seatList) {
        Assert.notNull(theaterId, "theaterId can not be null");

        this.theaterId = theaterId;
        this.seatList = seatList;
    }
}
