package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.repository.dao.*;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ScreeningRepositoryQuery {

    List<Screening> findScreeningAtTargetDate(LocalDate targetDate, Long cinemaId, @Nullable Long movieId);

    List<ScreeningDao.Preview> findPreviewAtTargetDate(LocalDate targetDate, Long cinemaId, @Nullable Long movieId);

    Page<ScreeningDao.AdminRow> findAdminRow(Pageable pageable, LocalDate targetDate, @Nullable Long theaterId, @Nullable Long movieId);

    Optional<ScreeningDao.AdminRowDetail> findAdminDetail(Long screeningId);

    boolean existsAlreadyUsing(Long theaterId, LocalDate date, LocalTime time);
}
