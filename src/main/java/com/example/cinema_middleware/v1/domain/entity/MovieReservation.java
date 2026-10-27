package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.DefaultEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.PaymentMethod;
import com.example.cinema_middleware.v1.domain.entity.enums.PaymentStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.support.exception.AlreadyPaidException;
import com.example.cinema_middleware.v1.support.exception.PaymentAmountMismatchException;
import com.example.cinema_middleware.v1.support.exception.ReservationExpiredException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_movie_reservation_payment", columnNames = "movie_payment_id"))
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class MovieReservation extends DefaultEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_reservation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_id")
    private Screening screening;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_issuance_id")
    private CouponIssuance couponIssuance;

    // 예매를 확정한(SUCCESS) 결제만 가리킨다. 결제 전·실패 상태에서는 null.
    // 결제 시도 목록은 컬렉션으로 두지 않고 MoviePayment 쪽에서 조회한다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_payment_id")
    private MoviePayment payment;

    @OneToMany(mappedBy = "reservation")
    private List<MovieReservationSeat> reservationSeatList = new ArrayList<>();

    // 서로 다른 결제 시도가 동시에 승인될 때 한 건만 확정되도록 하는 낙관적 락
    @Version
    private Long version;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String seatNames;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal originalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;

    @Column(updatable = false, nullable = false)
    private LocalDateTime expiredAt;

    public static MovieReservation create(Member member, Screening screening, String code, String seatNames, BigDecimal originalAmount) {
        Assert.notNull(member, "member can not be null");
        Assert.notNull(screening, "screening can not be null");
        Assert.hasText(code, "code can not be null");
        Assert.hasText(seatNames, "seatNames can not be null");
        Assert.notNull(originalAmount, "originalAmount can not be null");

        MovieReservation movieReservation = new MovieReservation();
        movieReservation.member = member;
        movieReservation.screening = screening;
        movieReservation.code = code;
        movieReservation.seatNames = seatNames;
        movieReservation.originalAmount = originalAmount;
        movieReservation.status = ReservationStatus.PENDING;
        movieReservation.expiredAt = LocalDateTime.now().plusMinutes(5);

        return movieReservation;
    }

    public void confirm() {
        LocalDateTime now = LocalDateTime.now();

        if (!ReservationStatus.PENDING.equals(this.getStatus())) {
            throw new IllegalStateException();
        }
        if (now.isAfter(this.expiredAt)) {
            throw new ReservationExpiredException();
        }

        this.status = ReservationStatus.SUCCESS;
        this.changeUpdatedAt(now);
    }

    /** seatNames 는 create 에서 ", " 로 이어 붙인 좌석명이므로 같은 구분자로 나눠 센다. */
    public int getSeatCount() {
        if (this.seatNames == null || this.seatNames.isBlank()) return 0;

        return this.seatNames.split(", ").length;
    }

    /** 결제 시도 생성. 이미 결제됐거나 결제 대기(PENDING)가 아닌 예매, 만료된 예매는 거부한다. */
    public MoviePayment startPayment(String idempotencyKey, PaymentMethod method) {
        if (this.payment != null || !ReservationStatus.PENDING.equals(this.status)) {
            throw new AlreadyPaidException();
        }
        if (LocalDateTime.now().isAfter(this.expiredAt)) {
            throw new ReservationExpiredException();
        }

        return MoviePayment.create(this, idempotencyKey, this.originalAmount, method);
    }

    /** 승인된 결제로 예매를 확정한다. 부분 결제는 금액 불일치로 거부한다. */
    public void completePayment(MoviePayment payment) {
        Assert.notNull(payment, "payment can not be null");

        if (this.payment != null) {
            throw new AlreadyPaidException();
        }
        // 다른 예매의 결제로 확정하는 것을 막는다. 프록시 비교를 피하려고 id 로 비교한다.
        if (!Objects.equals(payment.getReservation().getId(), this.id)) {
            throw new IllegalArgumentException("다른 예매의 결제입니다.");
        }
        if (!PaymentStatus.COMPLETE.equals(payment.getStatus())) {
            throw new IllegalStateException("승인되지 않은 결제입니다: " + payment.getStatus());
        }
        if (payment.getPaidAmount().compareTo(this.originalAmount) != 0) {
            throw new PaymentAmountMismatchException();
        }

        this.payment = payment;
        this.confirm();
    }
}
