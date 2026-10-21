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

    /**
     * 관리자 예매 목록 검색 조건.
     * 리포지토리 전용 타입(repository.dto.search)은 서비스가 변환해 넘긴다.
     */
    public record AdminSearch(
            Pageable pageable,
            LocalDate startDate,
            LocalDate endDate,
            @Nullable ReservationStatus status,
            @Nullable String code
    ) {
    }
}
