package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.service.dto.ScreeningSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import java.time.LocalDate;

public class ScreeningRepositoryImpl implements ScreeningRepositoryQuery {


    @Override
    public Page<ScreeningSummaryDto> findSummaryWithPage(LocalDate targetDate, Long movieId) {
        return null;
    }

    @Override
    public Slice<ScreeningSummaryDto> findSummaryWithSlice(LocalDate targetDate, Long movieId) {
        return null;
    }
}
