package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.DefaultEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;

import java.time.LocalDate;

@Entity
@SQLRestriction("status != 'HIDDEN'")
@SQLDelete(sql = "UPDATE movie SET status = 'HIDDEN' WHERE movie_id = ?")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class Movie extends DefaultEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private String summary;

    @Lob
    private String description;

    @Column(nullable = false)
    private Integer runningTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MovieAgeRating ageRating;

    @Column(nullable = false)
    private LocalDate releaseDate;

    @Column(nullable = false)
    private LocalDate closeDate;

    private String posterUrl;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MovieStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MovieCategory category;

    public static Movie create(
            String title,
            String summary,
            String description,
            Integer runningTime,
            MovieAgeRating ageRating,
            LocalDate releaseDate,
            LocalDate closeDate,
            MovieStatus status,
            MovieCategory category
    ) {
        Assert.hasText(title, "title can not be null");
        Assert.hasText(summary, "summary can not be null");
        Assert.notNull(runningTime, "runningTime can not be null");
        Assert.notNull(ageRating, "ageRating can not be null");
        Assert.notNull(releaseDate, "releaseDate can not be null");
        Assert.notNull(closeDate, "closeDate can not be null");
        Assert.notNull(status, "status can not be null");
        Assert.notNull(category, "category can not be null");

        Movie movie = new Movie();
        movie.title = title;
        movie.summary = summary;
        movie.description = description;
        movie.runningTime = runningTime;
        movie.ageRating = ageRating;
        movie.releaseDate = releaseDate;
        movie.closeDate = closeDate;
        movie.status = status;
        movie.category = category;

        return movie;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRunningTime(Integer runningTime) {
        this.runningTime = runningTime;
    }

    public void setAgeRating(MovieAgeRating ageRating) {
        this.ageRating = ageRating;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public void setCloseDate(LocalDate closeDate) {
        this.closeDate = closeDate;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public void setStatus(MovieStatus status) {
        this.status = status;
    }

    public void setCategory(MovieCategory category) {
        this.category = category;
    }
}
