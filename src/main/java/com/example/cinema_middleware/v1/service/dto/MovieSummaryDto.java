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

    public static MovieSummaryDto from(Movie movie) {
        MovieSummaryDto movieSummary = new MovieSummaryDto();
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
