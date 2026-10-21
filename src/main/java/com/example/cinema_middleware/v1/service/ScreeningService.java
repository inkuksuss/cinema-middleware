package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.repository.ScreeningRepository;
import com.example.cinema_middleware.v1.repository.dto.ScreeningSeat;
import com.example.cinema_middleware.v1.repository.dto.SeatCount;
import com.example.cinema_middleware.v1.service.dto.ScreeningDto;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final ScreeningRepository screeningRepository;

    public List<ScreeningDto.Summary> getScreeningSummaryByDate(LocalDate targetDate, @Nullable Long movieId) {
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

        // 상영마다 좌석 수 목록을 전부 순회하지 않도록 screeningId 로 한 번만 색인한다
        Map<Long, SeatCount> seatCountByScreeningId = seatCountList
                .stream()
                .collect(Collectors.toMap(SeatCount::getScreeningId, Function.identity()));

        return screeningList
                .stream()
                .map(screening -> ScreeningDto.Summary.of(screening, seatCountByScreeningId.get(screening.getId())))
                .toList();
    }

    public ScreeningDto.SeatMap getAllSeatForReservation(Long screeningId) {
        Assert.notNull(screeningId, "screeningId can not be null");

        List<ScreeningSeat> seatList = screeningRepository.findAllSeatForReservationById(screeningId);

        return ScreeningDto.SeatMap.of(screeningId, seatList);
    }
}
