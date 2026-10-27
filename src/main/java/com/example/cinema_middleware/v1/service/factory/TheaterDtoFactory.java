package com.example.cinema_middleware.v1.service.factory;

import com.example.cinema_middleware.v1.domain.entity.Cinema;
import com.example.cinema_middleware.v1.domain.entity.Theater;
import com.example.cinema_middleware.v1.domain.entity.TheaterSeat;
import com.example.cinema_middleware.v1.repository.dao.SeatDao;
import com.example.cinema_middleware.v1.service.dto.SeatDto;
import com.example.cinema_middleware.v1.service.dto.TheaterDto;
import org.springframework.util.Assert;

import java.util.List;

import static com.example.cinema_middleware.v1.service.factory.FactorySupport.nullSafe;

/**
 * 상영관 응답 DTO 조립기.
 * 엔티티 → {@link TheaterDto} 변환의 유일한 지점이다.
 */
public final class TheaterDtoFactory {

    private TheaterDtoFactory() {
    }

    public static TheaterDto.Summary summary(Theater theater) {
        Assert.notNull(theater, "theater can not be null");

        Cinema cinema = nullSafe(theater, Theater::getCinema);

        return new TheaterDto.Summary(
                theater.getId(),
                theater.getName(),
                theater.getStatus(),

                nullSafe(cinema, Cinema::getId),
                nullSafe(cinema, Cinema::getName),
                nullSafe(cinema, Cinema::getStatus)
        );
    }

    public static TheaterDto.ScreenMap screenMap(List<SeatDao.Info> infoList) {
        Assert.notEmpty(infoList, "infoList can not be empty");

        return new TheaterDto.ScreenMap(
                infoList.getFirst().screeningId(),
                infoList.getFirst().screeningStatus(),
                infoList.stream().map(info -> SeatDtoFactory.summary(info)).toList()
        );
    }
}
