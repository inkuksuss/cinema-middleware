package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.BaseEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@SQLRestriction("is_delete = 'N'")
@SQLDelete(sql = "UPDATE movie SET is_delete = 'Y' WHERE movie_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Movie extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private String summary;

    @Lob
    private String description;

    @Column(nullable = false, length = 100)
    private Integer runningTime;

    @Column(nullable = false, length = 100)
    private String ageRating;

    @Column(nullable = false)
    private LocalDate releaseDate;

    @Column(nullable = false)
    private LocalDate closeDate;

    private String posterUrl;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private MovieCategory category;

    public Movie(String title, String summary, Integer runningTime, String ageRating, LocalDate releaseDate, LocalDate closeDate, MovieCategory category) {
        this.title = title;
        this.summary = summary;
        this.runningTime = runningTime;
        this.ageRating = ageRating;
        this.releaseDate = releaseDate;
        this.closeDate = closeDate;
        this.category = category;
    }
}
