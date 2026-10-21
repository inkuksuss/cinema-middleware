package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.time.LocalDate;

@Getter
@ToString
public class MovieSummary {

    private Long id;

    private String title;

    private String summary;

    private Integer runningTime;

    private MovieAgeRating ageRating;

    private LocalDate releaseDate;

    private Long viewCount;

    private MovieCategory category;

    private String posterUrl;

    public static MovieSummary from(Movie movie) {
        Assert.notNull(movie, "movie can not be null");

        MovieSummary movieSummary = new MovieSummary();
        movieSummary.id = movie.getId();
        movieSummary.title = movie.getTitle();
        movieSummary.summary = movie.getSummary();
        movieSummary.runningTime = movie.getRunningTime();
        movieSummary.ageRating = movie.getAgeRating();
        movieSummary.releaseDate = movie.getReleaseDate();
        movieSummary.viewCount = movie.getViewCount();
        movieSummary.category = movie.getCategory();

        return movieSummary;
    }

    /** 조회 결과(row)처럼 엔티티 없이 식별 정보만 있을 때 사용 */
    public static MovieSummary of(Long id, String title) {
        MovieSummary movieSummary = new MovieSummary();
        movieSummary.id = id;
        movieSummary.title = title;

        return movieSummary;
    }
}
