package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.Theater;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
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
 *
 * <p>응답은 중첩 없는 평탄 구조다. 다른 도메인에서 온 필드는 출처를 드러내도록
 * {@code movie*} / {@code theater*} 접두사를 붙인다.
 * 단 컬렉션 필드(1:N)는 스칼라로 펼칠 수 없으므로 평탄화 대상이 아니다.
 *
 * <p>사용자용(Summary/Detail)은 잔여 좌석 수를 포함하고, 관리자용(AdminSummary/AdminDetail)은
 * 식별 정보 위주라 필드가 서로 달라 통합하지 않는다.
 * 상위 계층(controller)을 참조하지 않는다.
 */
public final class ScreeningDto {

    private ScreeningDto() {
    }

    /** 사용자 상영 목록 — 잔여 좌석 수 포함 */
    public record Summary(
            Long id,

            Long movieId,
            String movieTitle,
            String movieSummary,
            Integer movieRunningTime,
            MovieAgeRating movieAgeRating,
            LocalDate movieReleaseDate,
            Long movieViewCount,
            MovieCategory movieCategory,
            String moviePosterUrl,

            Long theaterId,
            String theaterName,

            LocalDate startDate,
            LocalTime startAt,
            ScreeningStatus status,
            Integer totalSeatCount,
            Integer soldOutSeatCount
    ) {
        /**
         * 좌석 수가 필요 없는 경우에 사용.
         * 현재 미사용 — ReservationDto 평탄화로 호출처가 사라졌다.
         */
        public static Summary from(Screening screening) {
            return of(screening, null);
        }

        /** 좌석 수는 호출부에서 screeningId 로 미리 찾아 넘긴다(없으면 0). */
        public static Summary of(Screening screening, @Nullable SeatCount seatCount) {
            Assert.notNull(screening, "screening can not be null");

            // soft delete 된 영화·상영관(@SQLRestriction)은 조인에서 탈락해 null 일 수 있다.
            // 지난 내역은 조회되어야 하므로 해당 항목만 비워 응답한다.
            Movie movie = screening.getMovie();
            Theater theater = screening.getTheater();

            return new Summary(
                    screening.getId(),

                    movie == null ? null : movie.getId(),
                    movie == null ? null : movie.getTitle(),
                    movie == null ? null : movie.getSummary(),
                    movie == null ? null : movie.getRunningTime(),
                    movie == null ? null : movie.getAgeRating(),
                    movie == null ? null : movie.getReleaseDate(),
                    movie == null ? null : movie.getViewCount(),
                    movie == null ? null : movie.getCategory(),
                    movie == null ? null : movie.getPosterUrl(),

                    theater == null ? null : theater.getId(),
                    theater == null ? null : theater.getName(),

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

            Long movieId,
            String movieTitle,
            String movieSummary,
            Integer movieRunningTime,
            MovieAgeRating movieAgeRating,
            LocalDate movieReleaseDate,
            Long movieViewCount,
            MovieCategory movieCategory,
            String moviePosterUrl,

            Long theaterId,
            String theaterName,

            LocalDate startDate,
            LocalTime startAt,
            ScreeningStatus status,
            Integer totalSeatCount,
            Integer soldOutSeatCount
    ) {
        public static Detail of(Screening screening, @Nullable SeatCount seatCount) {
            Assert.notNull(screening, "screening can not be null");

            // soft delete 된 영화·상영관은 조인에서 탈락해 null 일 수 있다(Summary 와 동일)
            Movie movie = screening.getMovie();
            Theater theater = screening.getTheater();

            return new Detail(
                    screening.getId(),

                    movie == null ? null : movie.getId(),
                    movie == null ? null : movie.getTitle(),
                    movie == null ? null : movie.getSummary(),
                    movie == null ? null : movie.getRunningTime(),
                    movie == null ? null : movie.getAgeRating(),
                    movie == null ? null : movie.getReleaseDate(),
                    movie == null ? null : movie.getViewCount(),
                    movie == null ? null : movie.getCategory(),
                    movie == null ? null : movie.getPosterUrl(),

                    theater == null ? null : theater.getId(),
                    theater == null ? null : theater.getName(),

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

            Long movieId,
            String movieTitle,
            String movieSummary,
            Integer movieRunningTime,
            MovieAgeRating movieAgeRating,
            LocalDate movieReleaseDate,
            Long movieViewCount,
            MovieCategory movieCategory,
            String moviePosterUrl,

            Long theaterId,
            String theaterName,

            LocalDate startDate,
            LocalTime startAt,
            ScreeningStatus status
    ) {
        /** row 가 식별 정보만 담고 있어 나머지 영화 필드는 null 이다(기존 동작 유지). */
        public static AdminSummary from(ScreeningAdminRow row) {
            Assert.notNull(row, "row can not be null");

            return new AdminSummary(
                    row.getScreeningId(),

                    row.getMovieId(),
                    row.getMovieTitle(),
                    null, null, null, null, null, null, null,

                    row.getTheaterId(),
                    row.getTheaterName(),

                    row.getStartDate(),
                    row.getStartAt(),
                    row.getStatus()
            );
        }

        // TODO: movie·theater 필드를 채우지 않아 null 로 응답된다(기존 동작 유지).
        //  findDetailById 가 movie·theater 를 JOIN FETCH 하므로 매핑만 추가하면 된다
        public static AdminSummary from(Screening screening) {
            Assert.notNull(screening, "screening can not be null");

            return new AdminSummary(
                    screening.getId(),

                    null, null, null, null, null, null, null, null, null,

                    null, null,

                    screening.getStartDate(),
                    screening.getStartAt(),
                    screening.getStatus()
            );
        }
    }

    /** 관리자 상영 상세 */
    public record AdminDetail(
            Long screeningId,

            Long movieId,
            String movieTitle,
            String movieSummary,
            Integer movieRunningTime,
            MovieAgeRating movieAgeRating,
            LocalDate movieReleaseDate,
            Long movieViewCount,
            MovieCategory movieCategory,
            String moviePosterUrl,

            Long theaterId,
            String theaterName,

            LocalDate startDate,
            LocalTime startAt,
            ScreeningStatus status
    ) {
        // TODO: movie·theater 필드를 채우지 않아 null 로 응답된다(기존 동작 유지).
        //  findDetailById 가 movie·theater 를 JOIN FETCH 하므로 매핑만 추가하면 된다
        public static AdminDetail from(Screening screening) {
            Assert.notNull(screening, "screening can not be null");

            return new AdminDetail(
                    screening.getId(),

                    null, null, null, null, null, null, null, null, null,

                    null, null,

                    screening.getStartDate(),
                    screening.getStartAt(),
                    screening.getStatus()
            );
        }
    }

    /** 예매용 좌석 배치 응답 — seatList 는 1:N 이라 평탄화 대상이 아니다 */
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
