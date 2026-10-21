package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.DefaultEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.repository.dto.SeatData;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
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

    @OneToMany(mappedBy = "reservation")
    private List<MovieReservationSeat> reservationSeatList = new ArrayList<>();

    @OneToMany(mappedBy = "reservation")
    private List<MoviePayment> paymentList = new ArrayList<>();

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

    public static MovieReservation create(Member member, Screening screening, List<SeatData> seatDataList) {
        Assert.notNull(member, "member can not be null");
        Assert.notNull(screening, "screening can not be null");
        Assert.notEmpty(seatDataList, "seatDataList can not be empty");

        MovieReservation movieReservation = new MovieReservation();
        movieReservation.member = member;
        movieReservation.screening = screening;
        movieReservation.code = UUID.randomUUID().toString();
        movieReservation.expiredAt = LocalDateTime.now().plusHours(1);
        movieReservation.seatNames = seatDataList.stream().map(s -> s.getSeatRow() + s.getSeatColumn()).collect(Collectors.joining(", "));
        movieReservation.totalPrice = seatDataList.stream().map(SeatData::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        movieReservation.status = ReservationStatus.PENDING;

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
        this.expiredAt = null;
        this.issueCode();
        this.changeUpdatedAt(now);
    }

    public int getSeatCount() {
        return this.seatNames.length() - this.seatNames.replace(", ", "").length();
    }

    private void issueCode() {
        this.code = UUID.randomUUID().toString();
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public void setCanceledAt(LocalDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }
}
