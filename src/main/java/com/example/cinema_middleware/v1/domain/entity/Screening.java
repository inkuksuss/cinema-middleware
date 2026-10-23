package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.DefaultEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreenType;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@SQLRestriction("status != 'DELETE'")
@SQLDelete(sql = "UPDATE screening SET status = 'DELETE' WHERE screening_id = ?")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class Screening extends DefaultEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "screening_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id")
    private Theater theater;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScreenType screenType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalTime startAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScreeningStatus status;

    public static Screening create(Movie movie, Theater theater, LocalDate startDate, LocalTime startAt, ScreeningStatus status) {
        Assert.notNull(movie, "movie can not be null");
        Assert.notNull(theater, "theater can not be null");
        Assert.notNull(startDate, "startDate can not be null");
        Assert.notNull(startAt, "startAt can not be null");
        Assert.notNull(status, "status can not be null");

        Screening screening = new Screening();
        screening.movie = movie;
        screening.theater = theater;
        screening.startDate = startDate;
        screening.startAt = startAt;
        screening.status = status;

        return screening;
    }

    public void setStatus(ScreeningStatus status) {
        this.status = status;
    }
}
