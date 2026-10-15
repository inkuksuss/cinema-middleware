package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.BaseEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.YesOrNo;
import com.example.cinema_middleware.v1.service.dto.AddMovieForm;
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
@NoArgsConstructor(access = AccessLevel.PUBLIC)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private YesOrNo isActive = YesOrNo.N;

    private String posterUrl;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private MovieCategory category;

    public static Movie create(AddMovieForm form) {
        if (form == null) throw new IllegalStateException();
        Movie movie = new Movie();
        movie.title = form.getTitle();
        movie.summary = form.getSummary();
        movie.description = form.getDescription();
        movie.runningTime = form.getRunningTime();
        movie.ageRating = form.getAgeRating();
        movie.releaseDate = form.getReleaseDate();
        movie.closeDate = form.getCloseDate();
        movie.isActive = form.getIsActive();

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

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public void setCloseDate(LocalDate closeDate) {
        this.closeDate = closeDate;
    }

    public void setIsActive(YesOrNo isActive) {
        this.isActive = isActive;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public void setCategory(MovieCategory category) {
        this.category = category;
    }
}
