package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter @Setter
@ToString
public class MovieSummary {

    private Long id;

    private String title;

    private String summary;

    private Integer runningTime;

    private String ageRating;

    private LocalDate releaseDate;

    private String posterUrl;

    private Long viewCount;

    private MovieCategory category;

    public static MovieSummary from(Movie movie) {
        MovieSummary movieSummary = new MovieSummary();
        movieSummary.id = movie.getId();
        movieSummary.title = movie.getTitle();
        movieSummary.summary = movie.getSummary();
        movieSummary.runningTime = movie.getRunningTime();
        movieSummary.ageRating = movie.getAgeRating();
        movieSummary.releaseDate = movie.getReleaseDate();
        movieSummary.posterUrl = movie.getPosterUrl();
        movieSummary.viewCount = movie.getViewCount();
        movieSummary.category = movie.getCategory();

        return movieSummary;
    }
}
