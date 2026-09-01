package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class MovieSummaryDto {

    private Long id;

    private String title;

    private String summary;

    private Integer runningTime;

    private String ageRating;

    private LocalDate releaseDate;

    private String posterUrl;

    private Long viewCount;

    private MovieCategory category;

    public MovieSummaryDto(Movie movie) {
        this.id = movie.getId();
        this.title = movie.getTitle();
        this.summary = movie.getSummary();
        this.runningTime = movie.getRunningTime();
        this.ageRating = movie.getAgeRating();
        this.releaseDate = movie.getReleaseDate();
        this.posterUrl = movie.getPosterUrl();
        this.viewCount = movie.getViewCount();
        this.category = movie.getCategory();
    }
}
