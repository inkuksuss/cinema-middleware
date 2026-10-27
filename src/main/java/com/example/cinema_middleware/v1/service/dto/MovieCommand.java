package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * 영화 도메인 서비스 입력 커맨드.
 */
public final class MovieCommand {

    private MovieCommand() {
    }

    public record Create(
            String title,
            String summary,
            String description,
            Integer runningTime,
            MovieAgeRating ageRating,
            LocalDate releaseDate,
            LocalDate closeDate,
            MovieStatus status,
            MovieCategory category,
            MultipartFile thumbnail
    ) {
    }

    public record Update(
            Long movieId,
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
    }
}
