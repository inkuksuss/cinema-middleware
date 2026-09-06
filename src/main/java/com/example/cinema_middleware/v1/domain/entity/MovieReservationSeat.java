package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.SimpleBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity

// 낙관적 락으로도 해결할 수 있지만 그 경우 남은 좌석을 계산하려면 movie_reservation 테이블을 조인에 포함시켜야한다.
@Table(
        uniqueConstraints = @UniqueConstraint(
        name = "uk_seat_hold",
        columnNames = {"screening_id", "theater_seat_id", "is_active"})
)
@SQLRestriction("is_delete = 'N'")
@SQLDelete(sql = "UPDATE movie_reservation_seat SET is_delete = 'Y' WHERE movie_reservation_seat_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MovieReservationSeat extends SimpleBaseEntity {

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

    private String isActive = "Y";

    public static MovieReservationSeat of(MovieReservation reservation, Screening screening, TheaterSeat theaterSeat) {
        MovieReservationSeat movieReservationSeat = new MovieReservationSeat();
        movieReservationSeat.reservation = reservation;
        movieReservationSeat.screening = screening;
        movieReservationSeat.theaterSeat = theaterSeat;

        return movieReservationSeat;
    }

    public void setReservation(MovieReservation reservation) {
        this.reservation = reservation;
    }

    /*
        좌석 만료 혹은 취소 시 null 값으로 바꾸어 unique 제약조건에 걸리지 않게 한다.
         */
    public void giveUpSeat() {
        this.isActive = null;
    }
}
