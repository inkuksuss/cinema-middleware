package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.*;

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
 * 상위 계층(controller)을 참조하지 않는다. 엔티티 변환은 {@code service.factory.ReservationDtoFactory} 가 담당한다.
 */
public final class ReservationDto {

    private ReservationDto() {
    }

    /** 사용자 예매 내역 목록 */
    public record Summary(
            Long reservationId,
            String code,
            BigDecimal totalPrice,
            ReservationStatus status,

            Long movieId,
            String movieTitle,
            String posterUrl,

            Long theaterId,
            String theaterName,

            Long screeningId,
            LocalDate screeningStartDate,
            LocalTime screeningStartAt,
            ScreeningStatus screeningStatus,

            Long cinemaId,
            String cinemaName
    ) {
    }

    /** 사용자 예매 상세 — 최신 결제 금액 포함 */
    public record Detail(
            Long reservationId,
            String code,
            BigDecimal totalPrice,
            String seatNames,
            ReservationStatus status,

            Long movieId,
            String movieTitle,

            Long theaterId,
            String theaterName,

            Long screeningId,
            LocalDate screeningStartDate,
            LocalTime screeningStartAt,
            ScreeningStatus screeningStatus,

            Long couponId,
            String couponName,

            Long cinemaId,
            String cinemaName,

            BigDecimal paidAmount,
            PaymentMethod method,
            LocalDateTime paidAt,
            LocalDateTime canceledAt,
            PaymentStatus paymentStatus
    ) {
    }

    /** 관리자 예매 목록 */
    public record AdminSummary(
            Long reservationId,
            String code,
            ReservationStatus status,

            Long memberId,
            String username,

            Long movieId,
            String movieTitle,

            Long theaterId,
            String theaterName,

            Long screeningId,
            LocalDate screeningStartDate,
            LocalTime screeningStartAt,
            ScreeningStatus screeningStatus,

            Long cinemaId,
            String cinemaName
    ) {
    }

    /** 관리자 예매 상세 */
    public record AdminDetail(
            Long reservationId,
            String code,
            BigDecimal totalPrice,
            String seatNames,
            ReservationStatus status,

            Long memberId,
            String username,

            Long movieId,
            String movieTitle,

            Long theaterId,
            String theaterName,

            Long screeningId,
            LocalDate screeningStartDate,
            LocalTime screeningStartAt,
            ScreeningStatus screeningStatus,

            Long couponId,
            String couponName,

            Long cinemaId,
            String cinemaName,

            BigDecimal paidAmount,
            PaymentMethod method,
            PaymentStatus paymentStatus,
            LocalDateTime paidAt,
            LocalDateTime canceledAt
    ) {
    }
}
