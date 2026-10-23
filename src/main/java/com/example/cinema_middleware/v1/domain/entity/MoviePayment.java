package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.DefaultEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.PaymentMethod;
import com.example.cinema_middleware.v1.domain.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 시도. 시도 1회가 행 1개이며, 재시도는 새 멱등키로 새 행을 만든다.
 * 예매를 확정한 결제는 {@link MovieReservation#getPayment()} 가 가리킨다.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_movie_payment_idempotency_key", columnNames = "idempotency_key"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MoviePayment extends DefaultEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_reservation_id", nullable = false)
    private MovieReservation reservation;

    @Column(nullable = false, length = 64, updatable = false)
    private String idempotencyKey;

    @Column(precision = 10, scale = 2, nullable = false, updatable = false)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    private LocalDateTime paidAt;

    private LocalDateTime canceledAt;

    static MoviePayment create(MovieReservation reservation, String idempotencyKey, BigDecimal paidAmount, PaymentMethod method) {
        Assert.notNull(reservation, "reservation can not be null");
        Assert.hasText(idempotencyKey, "idempotencyKey can not be empty");
        Assert.notNull(paidAmount, "paidAmount can not be null");
        Assert.notNull(method, "method can not be null");

        MoviePayment moviePayment = new MoviePayment();
        moviePayment.reservation = reservation;
        moviePayment.idempotencyKey = idempotencyKey;
        moviePayment.paidAmount = paidAmount;
        moviePayment.method = method;
        moviePayment.status = PaymentStatus.PENDING;

        return moviePayment;
    }

    /**
     * 결제 승인. 이미 COMPLETE 면 아무것도 하지 않고 false 를 반환한다(중복 콜백에 멱등).
     * @return 이번 호출로 승인됐으면 true
     */
    public boolean approve() {
        if (PaymentStatus.COMPLETE.equals(this.status)) return false;
        if (!PaymentStatus.PENDING.equals(this.status)) {
            throw new IllegalStateException("승인할 수 없는 결제 상태입니다: " + this.status);
        }

        this.status = PaymentStatus.COMPLETE;
        return true;
    }

    /** 결제 실패. PENDING 일 때만 FAILED 로 바뀌고, 그 외 상태에서는 무시한다. */
    public void fail() {
        if (PaymentStatus.PENDING.equals(this.status)) {
            this.status = PaymentStatus.FAILED;
        }
    }
}
