package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatStatus;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상영 좌석 도메인 서비스 응답 DTO 모음.
 */
public final class SeatCommand {

    private SeatCommand() {
    }

    public record Create(
            String seatRow,
            String seatColumn,
            SeatGrade grade,
            BigDecimal price,
            SeatStatus status
    ) {
    }

    public record Update(
            String seatRow,
            String seatColumn,
            SeatGrade grade,
            BigDecimal price,
            SeatStatus status
    ) {
    }
}
