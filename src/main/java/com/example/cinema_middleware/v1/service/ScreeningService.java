package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.repository.ScreeningRepository;
import com.example.cinema_middleware.v1.repository.dto.ScreeningSeat;
import com.example.cinema_middleware.v1.repository.dto.SeatCount;
import com.example.cinema_middleware.v1.service.dto.MovieSummary;
import com.example.cinema_middleware.v1.service.dto.ScreeningSummary;
import com.example.cinema_middleware.v1.service.dto.ScreeningSeatMapper;
import com.example.cinema_middleware.v1.service.dto.TheaterSummary;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final ScreeningRepository screeningRepository;

    public List<ScreeningSummary> getScreeningSummaryByDate(LocalDate targetDate, @Nullable Long movieId) {
        List<Screening> screeningList = screeningRepository.findScreeningListAtTargetDate(
                targetDate == null ? LocalDate.now() : targetDate,
                movieId
        );

        List<SeatCount> seatCountList = screeningRepository.findSeatCountListByIdList(
                screeningList
                        .stream()
                        .map(Screening::getId)
                        .toList()
        );

        return screeningList
                .stream()
                .map(screening -> {
                    ScreeningSummary summary = ScreeningSummary.from(screening);
                    summary.setSeatCount(seatCountList);
                    summary.setMovieSummary(MovieSummary.from(screening.getMovie()));
                    summary.setTheaterSummary(TheaterSummary.from(screening.getTheater()));

                    return summary;
                })
                .toList();
    }

    public ScreeningSeatMapper getAllSeatForReservation(Long screeningId) {
        Assert.notNull(screeningId, "screeningId can not be null");

        List<ScreeningSeat> seatList = screeningRepository.findAllSeatForReservationById(screeningId);

        return ScreeningSeatMapper.from(seatList);
    }
}
