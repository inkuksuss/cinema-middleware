package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.BaseEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@SQLRestriction("is_delete = 'N'")
@SQLDelete(sql = "UPDATE screening SET is_delete = 'Y' WHERE screening_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Screening extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "screening_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id")
    private Theater theater;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalTime startAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private ScreeningStatus status;

    public static Screening of(Movie movie, Theater theater, LocalDate startDate, LocalTime startAt, ScreeningStatus status) {
        Screening screening = new Screening();
        screening.movie = movie;
        screening.theater = theater;
        screening.startDate = startDate;
        screening.startAt = startAt;
        screening.status = status;

        return screening;
    }
}
