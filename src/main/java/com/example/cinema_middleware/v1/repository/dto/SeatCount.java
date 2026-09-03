package com.example.cinema_middleware.v1.repository.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@ToString
public class SeatCount {

    private Long screeningId;

    private Long totalSeatCount;

    private Long soldOutSeatCount;

    public SeatCount(Long screeningId, Long totalSeatCount, Long soldOutSeatCount) {
        this.screeningId = screeningId;
        this.totalSeatCount = totalSeatCount;
        this.soldOutSeatCount = soldOutSeatCount;
    }
}
