package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.MoviePayment;
import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import jakarta.annotation.Nullable;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 예매 도메인 서비스 응답 DTO 모음.
 * 사용자용은 영화·결제 정보를, 관리자용은 좌석명을 포함해 필드가 서로 달라 통합하지 않는다.
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
            MovieDto.Summary movieSummary,
            ScreeningDto.Summary screeningSummary
    ) {
        public static Summary from(MovieReservation movieReservation) {
            Assert.notNull(movieReservation, "movieReservation can not be null");

            // 숨김 처리된 영화(@SQLRestriction)의 상영은 조인에서 탈락해 screening 이 null 일 수 있다.
            // 지난 예매 내역은 영화가 내려가도 조회되어야 하므로 상영 정보만 비워 응답한다.
            Screening screening = movieReservation.getScreening();

            return new Summary(
                    movieReservation.getId(),
                    movieReservation.getCode(),
                    movieReservation.getTotalPrice(),
                    movieReservation.getSeatCount(),
                    movieReservation.getStatus(),
                    screening == null || screening.getMovie() == null
                            ? null : MovieDto.Summary.from(screening.getMovie()),
                    screening == null ? null : ScreeningDto.Summary.from(screening)
            );
        }
    }

    /** 사용자 예매 상세 — 최신 결제 정보 포함 */
    public record Detail(
            Long reservationId,
            String code,
            BigDecimal totalPrice,
            String seatNames,
            Integer seatCount,
            ReservationStatus status,
            LocalDateTime expiredAt,
            LocalDateTime canceledAt,
            PaymentSummary paymentSummary
    ) {
        /** 결제 정보가 아직 없는 경우(예: 예매 생성 직후) */
        public static Detail from(MovieReservation movieReservation) {
            return of(movieReservation, null);
        }

        /** 결제 내역이 없으면 paymentSummary 는 null 이다. */
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
                    latestPayment == null ? null : PaymentSummary.from(latestPayment)
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
            ScreeningDto.AdminSummary screeningSummary
    ) {
        // TODO: seatNames/screeningSummary 를 채우지 않아 null 로 응답된다(기존 동작 유지)
        public static AdminSummary from(MovieReservation movieReservation) {
            Assert.notNull(movieReservation, "movieReservation can not be null");

            return new AdminSummary(
                    movieReservation.getId(),
                    movieReservation.getCode(),
                    movieReservation.getTotalPrice(),
                    null,
                    movieReservation.getSeatCount(),
                    movieReservation.getStatus(),
                    null
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

    /** 결제 요약 */
    public record PaymentSummary(
            BigDecimal paidAmount
    ) {
        public static PaymentSummary from(MoviePayment moviePayment) {
            Assert.notNull(moviePayment, "moviePayment can not be null");

            return new PaymentSummary(moviePayment.getPaidAmount());
        }
    }
}
