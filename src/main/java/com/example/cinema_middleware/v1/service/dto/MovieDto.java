package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * 영화 도메인 서비스 응답 DTO 모음.
 */
public final class MovieDto {

    private MovieDto() {
    }

    /** 목록 조회용 요약 */
    public record Summary(
            Long id,
            String title,
            String summary,
            Integer runningTime,
            MovieAgeRating ageRating,
            LocalDate releaseDate,
            Long viewCount,
            String posterUrl,
            MovieCategory category,
            MovieStatus status
    ) {
    }

    /** 상세 조회 — 첨부파일 목록 포함 */
    public record Detail(
            Long id,
            String title,
            String summary,
            String description,
            Integer runningTime,
            MovieAgeRating ageRating,
            LocalDate releaseDate,
            LocalDate closeDate,
            String posterUrl,
            Long viewCount,
            MovieCategory category,
            MovieStatus status,
            List<FileDto.Summary> fileList
    ) {
    }
}
