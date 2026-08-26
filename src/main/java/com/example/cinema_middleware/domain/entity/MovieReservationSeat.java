package com.example.cinema_middleware.domain.entity;

import com.example.cinema_middleware.domain.entity.bases.SimpleBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
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

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    private String isActive;
}
