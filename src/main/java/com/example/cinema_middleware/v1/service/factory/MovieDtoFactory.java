package com.example.cinema_middleware.v1.service.factory;

import com.example.cinema_middleware.v1.domain.entity.FileMaster;
import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.service.dto.MovieDto;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 영화 응답 DTO 조립기.
 * 엔티티 → {@link MovieDto} 변환의 유일한 지점이다.
 */
public final class MovieDtoFactory {

    private MovieDtoFactory() {
    }

    public static MovieDto.Summary summary(Movie movie) {
        Assert.notNull(movie, "movie can not be null");

        return new MovieDto.Summary(
                movie.getId(),
                movie.getTitle(),
                movie.getSummary(),
                movie.getRunningTime(),
                movie.getAgeRating(),
                movie.getReleaseDate(),
                movie.getViewCount(),
                movie.getPosterUrl(),
                movie.getCategory(),
                movie.getStatus()
        );
    }

    public static MovieDto.Detail detail(Movie movie) {
        return detail(movie, List.of());
    }

    public static MovieDto.Detail detail(Movie movie, List<FileMaster> fileMasterList) {
        Assert.notNull(movie, "movie can not be null");
        Assert.notNull(fileMasterList, "fileMasterList can not be null");

        return new MovieDto.Detail(
                movie.getId(),
                movie.getTitle(),
                movie.getSummary(),
                movie.getDescription(),
                movie.getRunningTime(),
                movie.getAgeRating(),
                movie.getReleaseDate(),
                movie.getCloseDate(),
                movie.getPosterUrl(),
                movie.getViewCount(),
                movie.getCategory(),
                movie.getStatus(),
                fileMasterList.stream().map(FileDtoFactory::summary).toList()
        );
    }
}
