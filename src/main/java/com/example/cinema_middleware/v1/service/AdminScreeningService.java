package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.repository.ScreeningRepository;
import com.example.cinema_middleware.v1.service.dto.AdminScreeningDetail;
import com.example.cinema_middleware.v1.service.dto.AdminScreeningSummary;
import com.example.cinema_middleware.v1.service.dto.forms.UpdateScreeningForm;
import com.example.cinema_middleware.v1.support.exception.ScreeningHaveReservationException;
import com.example.cinema_middleware.v1.support.exception.TargetNotFoundException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class AdminScreeningService {

    private final ScreeningRepository screeningRepository;
    private final AdminMovieReservationService reservationService;

    public boolean isUsing(Long theaterId) {
        LocalDateTime now = LocalDateTime.now();
        return screeningRepository.existsAlreadyUsing(theaterId, now.toLocalDate(), now.toLocalTime());
    }

    public Page<AdminScreeningSummary> getSummary(
            Pageable pageable,
            LocalDate targetDate,
            @Nullable Long theaterId,
            @Nullable Long movieId
    ) {
        Assert.notNull(pageable, "pageable can not be null");
        Assert.notNull(targetDate, "targetDate can not be null");

        return screeningRepository.findAdminRow(
                pageable,
                targetDate,
                theaterId,
                movieId
                ).map(AdminScreeningSummary::from);
    }

    public AdminScreeningDetail getDetail(Long screeningId) {
        Assert.notNull(screeningId, "screeningId can not be null");

        Screening screening = screeningRepository.findDetailById(screeningId)
                .orElseThrow(() -> { throw new TargetNotFoundException(); });

        return AdminScreeningDetail.from(screening);
    }

    @Transactional
    public void update(UpdateScreeningForm form) {
        Assert.notNull(form, "updateScreeningForm can not be null");

        checkEditable(form.getScreeningId());

        Screening screening = screeningRepository.findById(form.getScreeningId())
                .orElseThrow(() -> { throw new TargetNotFoundException(); });
        screening.setStatus(form.getStatus());
    }

    public void remove(Long screeningId) {
        checkEditable(screeningId);

        screeningRepository.deleteById(screeningId);
    }

    private void checkEditable(Long screeningId) {
        Assert.notNull(screeningId, "screeningId can not be null");

        if (reservationService.checkAliveReservation(screeningId)) {
            throw new ScreeningHaveReservationException();
        }
    }
}
