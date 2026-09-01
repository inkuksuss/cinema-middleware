package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;

import java.time.LocalDate;

public class MovieDetailDto {

    private Long id;

    private String title;

    private String summary;

    private String description;

    private Integer runningTime;

    private String ageRating;

    private LocalDate releaseDate;

    private LocalDate closeDate;

    private String posterUrl;

    private Long viewCount;

    private MovieCategory category;

    public MovieDetailDto(Movie movie) {
        this.id = movie.getId();
        this.title = movie.getTitle();
        this.summary = movie.getSummary();
        this.description = movie.getDescription();
        this.runningTime = movie.getRunningTime();
        this.ageRating = movie.getAgeRating();
        this.releaseDate = movie.getReleaseDate();
        this.closeDate = movie.getCloseDate();
        this.posterUrl = movie.getPosterUrl();
        this.viewCount = movie.getViewCount();
        this.category = movie.getCategory();
    }
}
