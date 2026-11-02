package com.example.cinema_middleware.v1.repository.dao.search;

import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public final class ReservationCondition {

    private ReservationCondition() {}

    public record AdminRow(
            Pageable pageable,
            LocalDate startDate,
            LocalDate endDate,
            @Nullable ReservationStatus status,
            @Nullable UUID reservationId
    ) {
    }
}
