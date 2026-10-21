package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationSeatStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import com.example.cinema_middleware.v1.repository.dto.ScreeningAdminRow;
import com.example.cinema_middleware.v1.repository.dto.ScreeningSeat;
import com.example.cinema_middleware.v1.repository.dto.SeatCount;
import jakarta.annotation.Nullable;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 상영 도메인 서비스 응답 DTO 모음.
 * 사용자용(Summary/Detail)은 잔여 좌석 수를 포함하고, 관리자용(AdminSummary/AdminDetail)은
 * 식별 정보 위주라 필드가 서로 달라 통합하지 않는다.
 * 상위 계층(controller)을 참조하지 않는다.
 */
public final class ScreeningDto {

    private ScreeningDto() {
    }

    /** 사용자 상영 목록 — 잔여 좌석 수 포함 */
    public record Summary(
            Long id,
            MovieDto.Summary movieSummary,
            TheaterDto.Summary theaterSummary,
            LocalDate startDate,
            LocalTime startAt,
            ScreeningStatus status,
            Integer totalSeatCount,
            Integer soldOutSeatCount
    ) {
        /** 좌석 수가 필요 없는 경우(예: 예매 내역에 끼워 넣는 상영 정보) */
        public static Summary from(Screening screening) {
            return of(screening, null);
        }

        /** 좌석 수는 호출부에서 screeningId 로 미리 찾아 넘긴다(없으면 0). */
        public static Summary of(Screening screening, @Nullable SeatCount seatCount) {
            Assert.notNull(screening, "screening can not be null");

            return new Summary(
                    screening.getId(),
                    MovieDto.Summary.from(screening.getMovie()),
                    TheaterDto.Summary.from(screening.getTheater()),
                    screening.getStartDate(),
                    screening.getStartAt(),
                    screening.getStatus(),
                    seatCount == null ? 0 : seatCount.getTotalSeatCount().intValue(),
                    seatCount == null ? 0 : seatCount.getSoldOutSeatCount().intValue()
            );
        }
    }

    /** 사용자 상영 상세 — 잔여 좌석 수 포함 */
    public record Detail(
            Long id,
            MovieDto.Summary movieSummary,
            TheaterDto.Summary theaterSummary,
            LocalDate startDate,
            LocalTime startAt,
            ScreeningStatus status,
            Integer totalSeatCount,
            Integer soldOutSeatCount
    ) {
        public static Detail of(Screening screening, @Nullable SeatCount seatCount) {
            Assert.notNull(screening, "screening can not be null");

            return new Detail(
                    screening.getId(),
                    MovieDto.Summary.from(screening.getMovie()),
                    TheaterDto.Summary.from(screening.getTheater()),
                    screening.getStartDate(),
                    screening.getStartAt(),
                    screening.getStatus(),
                    seatCount == null ? 0 : seatCount.getTotalSeatCount().intValue(),
                    seatCount == null ? 0 : seatCount.getSoldOutSeatCount().intValue()
            );
        }
    }

    /** 관리자 상영 목록 */
    public record AdminSummary(
            Long screeningId,
            MovieDto.Summary movieSummary,
            TheaterDto.Summary theaterSummary,
            LocalDate startDate,
            LocalTime startAt,
            ScreeningStatus status
    ) {
        public static AdminSummary from(ScreeningAdminRow row) {
            Assert.notNull(row, "row can not be null");

            return new AdminSummary(
                    row.getScreeningId(),
                    MovieDto.Summary.of(row.getMovieId(), row.getMovieTitle()),
                    TheaterDto.Summary.of(row.getTheaterId(), row.getTheaterName()),
                    row.getStartDate(),
                    row.getStartAt(),
                    row.getStatus()
            );
        }

        // TODO: movieSummary/theaterSummary 를 채우지 않아 null 로 응답된다(기존 동작 유지).
        //  findDetailById 가 movie·theater 를 JOIN FETCH 하므로 매핑만 추가하면 된다
        public static AdminSummary from(Screening screening) {
            Assert.notNull(screening, "screening can not be null");

            return new AdminSummary(
                    screening.getId(),
                    null,
                    null,
                    screening.getStartDate(),
                    screening.getStartAt(),
                    screening.getStatus()
            );
        }
    }

    /** 관리자 상영 상세 */
    public record AdminDetail(
            Long screeningId,
            MovieDto.Summary movieSummary,
            TheaterDto.Summary theaterSummary,
            LocalDate startDate,
            LocalTime startAt,
            ScreeningStatus status
    ) {
        // TODO: movieSummary/theaterSummary 를 채우지 않아 null 로 응답된다(기존 동작 유지).
        //  findDetailById 가 movie·theater 를 JOIN FETCH 하므로 매핑만 추가하면 된다
        public static AdminDetail from(Screening screening) {
            Assert.notNull(screening, "screening can not be null");

            return new AdminDetail(
                    screening.getId(),
                    null,
                    null,
                    screening.getStartDate(),
                    screening.getStartAt(),
                    screening.getStatus()
            );
        }
    }

    /** 예매용 좌석 배치 응답 */
    public record SeatMap(
            Long screeningId,
            List<Seat> seatList
    ) {
        public static SeatMap of(Long screeningId, List<ScreeningSeat> screeningSeatList) {
            Assert.notNull(screeningId, "screeningId can not be null");
            Assert.notNull(screeningSeatList, "screeningSeatList can not be null");

            return new SeatMap(
                    screeningId,
                    screeningSeatList.stream().map(Seat::from).toList()
            );
        }

        public record Seat(
                Long theaterSeatId,
                String seatRow,
                String seatColumn,
                SeatGrade grade,
                BigDecimal price,
                Long reservationSeatId,
                ReservationSeatStatus status
        ) {
            public static Seat from(ScreeningSeat screeningSeat) {
                Assert.notNull(screeningSeat, "screeningSeat can not be null");

                return new Seat(
                        screeningSeat.getTheaterSeatId(),
                        screeningSeat.getSeatRow(),
                        screeningSeat.getSeatColumn(),
                        screeningSeat.getSeatGrade(),
                        screeningSeat.getPrice(),
                        screeningSeat.getReservationSeatId(),
                        screeningSeat.getStatus()
                );
            }
        }
    }
}
