package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.BaseEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private ScreeningStatus status;

    public Screening(Movie movie, Theater theater, LocalDateTime startAt, LocalDateTime endAt, BigDecimal price, ScreeningStatus status) {
        this.movie = movie;
        this.theater = theater;
        this.startAt = startAt;
        this.endAt = endAt;
        this.price = price;
        this.status = status;
    }
}
