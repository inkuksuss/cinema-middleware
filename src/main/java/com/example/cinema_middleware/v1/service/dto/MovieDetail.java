package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter @Setter
@ToString
public class MovieDetail {

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

    public static MovieDetail from(Movie movie) {
        MovieDetail movieDetail = new MovieDetail();
        movieDetail.id = movie.getId();
        movieDetail.title = movie.getTitle();
        movieDetail.summary = movie.getSummary();
        movieDetail.description = movie.getDescription();
        movieDetail.runningTime = movie.getRunningTime();
        movieDetail.ageRating = movie.getAgeRating();
        movieDetail.releaseDate = movie.getReleaseDate();
        movieDetail.closeDate = movie.getCloseDate();
        movieDetail.posterUrl = movie.getPosterUrl();
        movieDetail.viewCount = movie.getViewCount();
        movieDetail.category = movie.getCategory();

        return movieDetail;
    }
}
