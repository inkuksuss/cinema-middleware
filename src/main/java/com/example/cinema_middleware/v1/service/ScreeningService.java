package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.repository.ScreeningRepository;
import com.example.cinema_middleware.v1.repository.dto.ScreeningSeat;
import com.example.cinema_middleware.v1.repository.dto.SeatCount;
import com.example.cinema_middleware.v1.service.dto.MovieSummary;
import com.example.cinema_middleware.v1.service.dto.ScreeningSummary;
import com.example.cinema_middleware.v1.service.dto.SeatMapper;
import com.example.cinema_middleware.v1.service.dto.TheaterSummary;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final ScreeningRepository screeningRepository;

    public List<ScreeningSummary> getScreeningSummaryByDate(LocalDate targetDate, @Nullable Long movieId) {
        List<Screening> screeningList = screeningRepository.findScreeningListAtTargetDate(targetDate, movieId);

        List<SeatCount> seatCountList = screeningRepository.findSeatCountListByIdList(
                screeningList.stream()
                        .map(Screening::getId)
                        .toList()
        );

        return screeningList.stream()
                .map(screening -> {
                    ScreeningSummary summary = ScreeningSummary.from(screening);
                    summary.addSeatCount(seatCountList);
                    summary.setMovieSummary(MovieSummary.from(screening.getMovie()));
                    summary.setTheaterSummary(TheaterSummary.from(screening.getTheater()));

                    return summary;
                })
                .toList();
    }

    public SeatMapper getScreeningSeat(Long screeningId) {
        List<ScreeningSeat> screeningSeatList = screeningRepository.findAllSeatByScreeningId(screeningId);

        if (screeningSeatList.size() == 0) {
            throw new IllegalStateException("좌석 정보가 존재하지 않습니다.");
        }

        return SeatMapper.from(screeningSeatList);
    }
}
