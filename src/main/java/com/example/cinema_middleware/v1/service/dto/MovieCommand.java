package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * 영화 도메인 서비스 입력 커맨드.
 * 생성 시점에 모든 값이 채워지므로 불완전한 객체가 존재하지 않는다.
 * 상위 계층(controller)을 참조하지 않는다 — 변환은 Request 쪽에서 한다.
 */
public final class MovieCommand {

    private MovieCommand() {
    }

    /** 영화 등록 — thumbnail 은 multipart 로 들어온다 */
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

    /** 영화 수정 — movieId 는 PathVariable 로 들어온다 */
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
