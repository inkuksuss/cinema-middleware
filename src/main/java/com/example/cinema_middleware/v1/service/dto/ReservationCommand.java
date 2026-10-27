package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * 예매 도메인 서비스 입력 커맨드.
 * 상위 계층(controller)을 참조하지 않는다 — 변환은 Request 쪽에서 한다.
 */
public final class ReservationCommand {

    private ReservationCommand() {
    }

    public record SearchAdminRow(
            Pageable pageable,
            LocalDate startDate,
            LocalDate endDate,
            @Nullable ReservationStatus status,
            @Nullable String code
    ) {
    }
}
