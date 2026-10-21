package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.HistoryEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MovieReservationHistory extends HistoryEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_reservation_history_id")
    private Long id;

    @Column(name = "movie_reservation_id", nullable = false)
    private Long movieReservationId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "screening_id", nullable = false)
    private Long screeningId;

    @Column(name = "coupon_issuance_id")
    private Long couponIssuanceId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String seatNames;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    private LocalDateTime expiredAt;

    private LocalDateTime canceledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private ReservationStatus status;

    public static MovieReservationHistory create(MovieReservation reservation) {
        Assert.notNull(reservation, "reservation can not be null");
        Assert.notNull(reservation.getMember(), "member can not be null");
        Assert.notNull(reservation.getScreening(), "screening can not be null");

        MovieReservationHistory history = new MovieReservationHistory();
        history.movieReservationId = reservation.getId();
        history.memberId = reservation.getMember().getId();
        history.screeningId = reservation.getScreening().getId();
        if (reservation.getCouponIssuance() != null) {
            history.couponIssuanceId = reservation.getCouponIssuance().getId();
        }
        history.code = reservation.getCode();
        history.seatNames = reservation.getSeatNames();
        history.totalPrice = reservation.getTotalPrice();
        history.status = reservation.getStatus();
        history.canceledAt = reservation.getCanceledAt();
        history.expiredAt = reservation.getExpiredAt();

        return history;
    }
}
