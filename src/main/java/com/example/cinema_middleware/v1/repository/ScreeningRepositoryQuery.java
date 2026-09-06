package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.repository.dto.ScreeningSeat;
import com.example.cinema_middleware.v1.repository.dto.SeatCount;
import com.example.cinema_middleware.v1.repository.dto.SeatPrice;
import jakarta.annotation.Nullable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScreeningRepositoryQuery {

    List<Screening> findScreeningListAtTargetDate(LocalDate targetDate, @Nullable Long movieId);

    List<SeatCount> findSeatCountListByIdList(@Param("screeningIdList") List<Long> screeningIdList);

    List<ScreeningSeat> findAllSeatByScreeningId(@Param("screeningId") Long screeningId);

    List<SeatPrice> findSeatPriceListFor(@Param("screeningId") Long screeningId, @Param("theaterSeatIdList") List<Long> theaterSeatIdList);
}
