package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.repository.ScreeningRepository;
import com.example.cinema_middleware.v1.repository.dao.ScreeningDao;
import com.example.cinema_middleware.v1.repository.dao.SeatDao;
import com.example.cinema_middleware.v1.service.dto.ScreeningDto;
import com.example.cinema_middleware.v1.service.dto.SeatDto;
import com.example.cinema_middleware.v1.service.dto.TheaterDto;
import com.example.cinema_middleware.v1.service.factory.ScreeningDtoFactory;
import com.example.cinema_middleware.v1.service.factory.SeatDtoFactory;
import com.example.cinema_middleware.v1.service.factory.TheaterDtoFactory;
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
    private final SeatService seatService;


    public List<ScreeningDto.Summary> getSummaryByDate(LocalDate targetDate, Long cinemaId, @Nullable Long movieId) {
        Assert.notNull(targetDate, "targetDate can not be null");
        Assert.notNull(cinemaId, "cinemaId can not be null");

        // need cache
        List<ScreeningDao.Preview> previewList = screeningRepository.findPreviewAtTargetDate(targetDate, cinemaId, movieId);

        return ScreeningDtoFactory.summaryList(previewList);
    }

    public TheaterDto.ScreenMap getScreenMapForReservation(Long screeningId) {
        Assert.notNull(screeningId, "screeningId can not be null");

        List<SeatDao.Info> infoList = seatService.getAllOfScreening(screeningId);

        return TheaterDtoFactory.screenMap(infoList);
    }

    public Screening getReferById(Long id) {
        Assert.notNull(id, "screeningId can not be null");

        return screeningRepository.getReferenceById(id);
    }
}
