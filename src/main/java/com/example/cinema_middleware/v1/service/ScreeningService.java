package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.repository.ScreeningRepository;
import com.example.cinema_middleware.v1.service.dto.ScreeningSummaryDto;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final ScreeningRepository screeningRepository;

    public Page<ScreeningSummaryDto> getSummaryByDate(LocalDate targetDate, @Nullable Long movieId) {
        screeningRepository.findSummaryWithPage(targetDate, movieId);

        return null;
    }
}
