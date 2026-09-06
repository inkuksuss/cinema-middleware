package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.BaseEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.support.exception.ReservationExpiredException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@SQLRestriction("is_delete = 'N'")
@SQLDelete(sql = "UPDATE movie_reservation SET is_delete = 'Y' WHERE movie_reservation_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MovieReservation extends BaseEntity {

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

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private Integer seatCount;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private ReservationStatus status;

    private LocalDateTime expiredAt;

    private LocalDateTime canceledAt;

    public static MovieReservation of(Member member, Screening screening, List<BigDecimal> priceList) {
        MovieReservation movieReservation = new MovieReservation();
        movieReservation.member = member;
        movieReservation.screening = screening;
        movieReservation.code = UUID.randomUUID().toString();
        movieReservation.seatCount = priceList.size();
        movieReservation.totalPrice = priceList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        movieReservation.status = ReservationStatus.PENDING;
        movieReservation.expiredAt = LocalDateTime.now().plusHours(1);

        return movieReservation;
    }

    public void confirm() {
        LocalDateTime now = LocalDateTime.now();

        if (!ReservationStatus.PENDING.equals(this.status)) {
            throw new IllegalStateException();
        }
        if (now.isAfter(this.expiredAt)) {
            throw new ReservationExpiredException();
        }

        this.status = ReservationStatus.SUCCESS;
        this.expiredAt = null;
        this.issueCode();
        this.changeUpdatedAt(now);
    }

    private void issueCode() {
        this.code = UUID.randomUUID().toString();
    }
}
