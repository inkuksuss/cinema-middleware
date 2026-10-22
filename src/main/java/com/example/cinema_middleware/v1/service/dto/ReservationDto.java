package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.MoviePayment;
import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.Theater;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import jakarta.annotation.Nullable;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 예매 도메인 서비스 응답 DTO 모음.
 *
 * <p>응답은 중첩 없는 평탄 구조다. 다른 도메인에서 온 필드는 출처를 드러내도록
 * {@code movie*} / {@code theater*} / {@code screening*} 접두사를 붙인다.
 * 단 컬렉션 필드(1:N)는 스칼라로 펼칠 수 없으므로 평탄화 대상이 아니다.
 *
 * <p>사용자용은 영화·결제 정보를, 관리자용은 좌석명을 포함해 필드가 서로 달라 통합하지 않는다.
 * 상위 계층(controller)을 참조하지 않는다.
 */
public final class ReservationDto {

    private ReservationDto() {
    }

    /** 사용자 예매 내역 목록 */
    public record Summary(
            Long reservationId,
            String code,
            BigDecimal totalPrice,
            Integer seatCount,
            ReservationStatus status,

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

            // 예매 자신의 status 와 겹치므로 상영 쪽에 접두사를 붙인다
            Long screeningId,
            LocalDate screeningStartDate,
            LocalTime screeningStartAt,
            ScreeningStatus screeningStatus,
            Integer totalSeatCount,
            Integer soldOutSeatCount
    ) {
        public static Summary from(MovieReservation movieReservation) {
            Assert.notNull(movieReservation, "movieReservation can not be null");

            // 숨김 처리된 영화(@SQLRestriction)의 상영은 조인에서 탈락해 screening 이 null 일 수 있다.
            // 지난 예매 내역은 영화가 내려가도 조회되어야 하므로 해당 항목만 비워 응답한다.
            Screening screening = movieReservation.getScreening();
            Movie movie = screening == null ? null : screening.getMovie();
            Theater theater = screening == null ? null : screening.getTheater();

            return new Summary(
                    movieReservation.getId(),
                    movieReservation.getCode(),
                    movieReservation.getTotalPrice(),
                    movieReservation.getSeatCount(),
                    movieReservation.getStatus(),

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

                    screening == null ? null : screening.getId(),
                    screening == null ? null : screening.getStartDate(),
                    screening == null ? null : screening.getStartAt(),
                    screening == null ? null : screening.getStatus(),
                    // 예매 내역에는 좌석 수를 조회하지 않는다(기존 ScreeningDto.Summary.from 과 동일하게 0)
                    0,
                    0
            );
        }
    }

    /** 사용자 예매 상세 — 최신 결제 금액 포함 */
    public record Detail(
            Long reservationId,
            String code,
            BigDecimal totalPrice,
            String seatNames,
            Integer seatCount,
            ReservationStatus status,
            LocalDateTime expiredAt,
            LocalDateTime canceledAt,
            BigDecimal paidAmount
    ) {
        /** 결제 정보가 아직 없는 경우(예: 예매 생성 직후) */
        public static Detail from(MovieReservation movieReservation) {
            return of(movieReservation, null);
        }

        /** 결제 내역이 없으면 paidAmount 는 null 이다. */
        public static Detail of(MovieReservation movieReservation, @Nullable MoviePayment latestPayment) {
            Assert.notNull(movieReservation, "movieReservation can not be null");

            return new Detail(
                    movieReservation.getId(),
                    movieReservation.getCode(),
                    movieReservation.getTotalPrice(),
                    movieReservation.getSeatNames(),
                    movieReservation.getSeatCount(),
                    movieReservation.getStatus(),
                    movieReservation.getExpiredAt(),
                    movieReservation.getCanceledAt(),
                    latestPayment == null ? null : latestPayment.getPaidAmount()
            );
        }
    }

    /** 관리자 예매 목록 */
    public record AdminSummary(
            Long reservationId,
            String code,
            BigDecimal totalPrice,
            String seatNames,
            Integer seatCount,
            ReservationStatus status,

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

            LocalDate screeningStartDate,
            LocalTime screeningStartAt,
            ScreeningStatus screeningStatus
    ) {
        // TODO: seatNames 와 상영 관련 필드를 채우지 않아 null 로 응답된다(기존 동작 유지)
        public static AdminSummary from(MovieReservation movieReservation) {
            Assert.notNull(movieReservation, "movieReservation can not be null");

            return new AdminSummary(
                    movieReservation.getId(),
                    movieReservation.getCode(),
                    movieReservation.getTotalPrice(),
                    null,
                    movieReservation.getSeatCount(),
                    movieReservation.getStatus(),

                    null,

                    null, null, null, null, null, null, null, null, null,

                    null, null,

                    null, null, null
            );
        }
    }

    /** 관리자 예매 상세 */
    public record AdminDetail(
            Long reservationId,
            String code,
            BigDecimal totalPrice,
            String seatNames,
            Integer seatCount,
            ReservationStatus status,
            LocalDateTime expiredAt,
            LocalDateTime canceledAt
    ) {
        public static AdminDetail from(MovieReservation movieReservation) {
            Assert.notNull(movieReservation, "movieReservation can not be null");

            return new AdminDetail(
                    movieReservation.getId(),
                    movieReservation.getCode(),
                    movieReservation.getTotalPrice(),
                    movieReservation.getSeatNames(),
                    movieReservation.getSeatCount(),
                    movieReservation.getStatus(),
                    movieReservation.getExpiredAt(),
                    movieReservation.getCanceledAt()
            );
        }
    }

    /**
     * 결제 요약.
     * 현재 미사용 — 평탄화로 Detail.paidAmount 가 되면서 호출처가 사라졌다.
     */
    public record PaymentSummary(
            BigDecimal paidAmount
    ) {
        public static PaymentSummary from(MoviePayment moviePayment) {
            Assert.notNull(moviePayment, "moviePayment can not be null");

            return new PaymentSummary(moviePayment.getPaidAmount());
        }
    }
}
