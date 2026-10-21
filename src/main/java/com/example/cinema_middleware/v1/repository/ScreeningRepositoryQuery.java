package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.repository.dto.ScreeningAdminRow;
import com.example.cinema_middleware.v1.repository.dto.ScreeningSeat;
import com.example.cinema_middleware.v1.repository.dto.SeatCount;
import com.example.cinema_middleware.v1.repository.dto.SeatData;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ScreeningRepositoryQuery {

    List<Screening> findScreeningListAtTargetDate(LocalDate targetDate, @Nullable Long movieId);

    List<SeatCount> findSeatCountListByIdList(List<Long> screeningIdList);

    List<ScreeningSeat> findAllSeatForReservationById(Long screeningId);

    List<SeatData> findSeatDataListFor(Long screeningId, List<Long> theaterSeatIdList);

    Page<ScreeningAdminRow> findAdminRow(Pageable pageable, LocalDate targetDate, @Nullable Long theaterId, @Nullable Long movieId);

    boolean existsAlreadyUsing(Long theaterId, LocalDate date, LocalTime time);
}
