package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.HistoryEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.PaymentMethod;
import com.example.cinema_middleware.v1.domain.entity.enums.PaymentStatus;
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
public class MoviePaymentHistory extends HistoryEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_payment_history_id")
    private Long id;

    @Column(name = "movie_reservation_id")
    private Long reservationId;

    @Column(nullable = false, length = 64, updatable = false)
    private String idempotencyKey;

    @Column(precision = 10, scale = 2, nullable = false, updatable = false)
    private BigDecimal paidAmount;

    @Column(nullable = false, length = 100)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    public static MoviePaymentHistory create(MoviePayment payment) {
        Assert.notNull(payment, "payment can not be null");
        Assert.notNull(payment.getReservation(), "reservation can not be null");

        MoviePaymentHistory history = new MoviePaymentHistory();
        history.reservationId = payment.getReservation().getId();
        history.idempotencyKey = payment.getIdempotencyKey();
        history.paidAmount = payment.getPaidAmount();
        history.method = payment.getMethod();
        history.status = payment.getStatus();

        return history;
    }
}
