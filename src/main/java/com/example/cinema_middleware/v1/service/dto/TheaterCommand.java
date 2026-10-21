package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;

import java.util.List;

/**
 * 상영관 도메인 서비스 입력 커맨드.
 * 상위 계층(controller)을 참조하지 않는다 — 변환은 Request 쪽에서 한다.
 */
public final class TheaterCommand {

    private TheaterCommand() {
    }

    /** 좌석 일괄 등록 — theaterId 는 PathVariable 로 들어온다 */
    public record BulkAddSeat(
            Long theaterId,
            List<Seat> seatList
    ) {
        public record Seat(
                String seatRow,
                String seatColumn,
                SeatGrade grade
        ) {
        }
    }
}
