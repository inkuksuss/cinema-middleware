package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.TheaterStatus;

/**
 * 상영관 도메인 서비스 응답 DTO 모음.
 */
public final class TheaterDto {

    private TheaterDto() {
    }

    /** 상영관 요약 */
    public record Summary(
            Long id,
            String name,
            TheaterStatus status
    ) {
    }
}
