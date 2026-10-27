package com.example.cinema_middleware.v1.service.factory;

import com.example.cinema_middleware.v1.domain.entity.TheaterSeat;
import com.example.cinema_middleware.v1.repository.dao.SeatDao;
import com.example.cinema_middleware.v1.service.dto.SeatDto;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 상영 좌석 응답 DTO 조립기.
 * 엔티티·조회 결과(repository.dto) → {@link SeatDto} 변환의 유일한 지점이다.
 */
public final class SeatDtoFactory {

    private SeatDtoFactory() {
    }

    public static SeatDto.Summary summary(SeatDao.Info info) {
        Assert.notNull(info, "info can not be null");

        return new SeatDto.Summary(
                info.theaterSeatId(),
                info.seatRow() + info.seatColumn(),
                info.grade(),
                info.price(),
                info.seatStatus(),

                info.reservationSeatId(),
                info.reservationSeatStatus()
        );
    }

    public static SeatDto.Summary summary(TheaterSeat theaterSeat) {
        Assert.notNull(theaterSeat, "theaterSeat can not be null");

        return new SeatDto.Summary(
                theaterSeat.getId(),
                theaterSeat.getSeatRow() + theaterSeat.getSeatColumn(),
                theaterSeat.getGrade(),
                theaterSeat.getPrice(),
                theaterSeat.getStatus(),

                null, null
        );
    }
}
