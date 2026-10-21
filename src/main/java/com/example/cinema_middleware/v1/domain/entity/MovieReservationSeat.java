package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.DefaultEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationSeatStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.Assert;


@Entity

// 낙관적 락으로도 해결할 수 있지만 그 경우 남은 좌석을 계산하려면 movie_reservation 테이블을 조인에 포함시켜야한다.
@Table(
        uniqueConstraints = @UniqueConstraint(
        name = "uk_seat_hold",
        columnNames = {"screening_id", "theater_seat_id", "status"})
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class MovieReservationSeat extends DefaultEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_reservation_seat_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_reservation_id")
    private MovieReservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_id")
    private Screening screening;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_seat_id")
    private TheaterSeat theaterSeat;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private ReservationSeatStatus status;

    public static MovieReservationSeat create(MovieReservation reservation, Screening screening, TheaterSeat theaterSeat) {
        Assert.notNull(reservation, "reservation can not be null");
        Assert.notNull(screening, "screening can not be null");
        Assert.notNull(theaterSeat, "theaterSeat can not be null");

        MovieReservationSeat movieReservationSeat = new MovieReservationSeat();
        movieReservationSeat.reservation = reservation;
        movieReservationSeat.screening = screening;
        movieReservationSeat.theaterSeat = theaterSeat;
        movieReservationSeat.status = ReservationSeatStatus.RESERVED;

        return movieReservationSeat;
    }

    public void setReservation(MovieReservation reservation) {
        this.reservation = reservation;
    }

    /*
        좌석 만료 혹은 취소 시 status 를 null 로 바꾸어 uk_seat_hold 유니크 제약에 걸리지 않게 한다.
        (MySQL 은 유니크 제약에서 NULL 을 서로 다른 값으로 취급한다)
     */
    public void giveUpSeat() {
        this.status = null;
    }
}
