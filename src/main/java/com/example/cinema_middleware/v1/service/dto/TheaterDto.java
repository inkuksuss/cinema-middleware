package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Theater;
import com.example.cinema_middleware.v1.domain.entity.TheaterSeat;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상영관 도메인 서비스 응답 DTO 모음.
 * 관리자/사용자 응답 필드가 동일해 하나를 공유한다.
 * 상위 계층(controller)을 참조하지 않는다.
 */
public final class TheaterDto {

    private TheaterDto() {
    }

    /** 상영관 요약 */
    public record Summary(
            Long id,
            String name
    ) {
        public static Summary from(Theater theater) {
            Assert.notNull(theater, "theater can not be null");

            return new Summary(theater.getId(), theater.getName());
        }

        /** 조회 결과(row)처럼 엔티티 없이 식별 정보만 있을 때 사용 */
        public static Summary of(Long id, String name) {
            return new Summary(id, name);
        }
    }

    /** 좌석 요약 */
    public record SeatSummary(
            Long id,
            String seatRow,
            String seatColumn,
            SeatGrade grade,
            BigDecimal price
    ) {
        public static SeatSummary from(TheaterSeat theaterSeat) {
            Assert.notNull(theaterSeat, "theaterSeat can not be null");

            return new SeatSummary(
                    theaterSeat.getId(),
                    theaterSeat.getSeatRow(),
                    theaterSeat.getSeatColumn(),
                    theaterSeat.getGrade(),
                    theaterSeat.getPrice()
            );
        }
    }

    /** 상영관 좌석 배치 응답 */
    public record SeatMap(
            Long theaterId,
            List<SeatSummary> seatList
    ) {
        public SeatMap {
            Assert.notNull(theaterId, "theaterId can not be null");
        }
    }
}
