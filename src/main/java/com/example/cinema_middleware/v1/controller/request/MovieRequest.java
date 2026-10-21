package com.example.cinema_middleware.v1.controller.request;

import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieStatus;
import com.example.cinema_middleware.v1.service.dto.MovieCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * 영화 API 요청 DTO 모음.
 * Request -> Command 변환은 이 계층에서 한다(controller -> service 단방향 유지).
 */
public final class MovieRequest {

    private MovieRequest() {
    }

    public record Create(
            @NotBlank(message = "title은(는) 필수 입력 항목입니다.")
            String title,

            @NotBlank(message = "summary은(는) 필수 입력 항목입니다.")
            String summary,

            String description,

            @NotNull(message = "runningTime은(는) 필수 입력 항목입니다.")
            @Min(message = "상영시간은 0보다 작을 수 없습니다.", value = 0)
            Integer runningTime,

            @NotNull(message = "ageRating은(는) 필수 입력 항목입니다.")
            MovieAgeRating ageRating,

            @NotNull(message = "releaseDate은(는) 필수 입력 항목입니다.")
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate releaseDate,

            @NotNull(message = "closeDate은(는) 필수 입력 항목입니다.")
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate closeDate,

            @NotNull(message = "status은(는) 필수 입력 항목입니다.")
            MovieStatus status,

            @NotNull(message = "category은(는) 필수 입력 항목입니다.")
            MovieCategory category
    ) {
        /** thumbnail 은 요청 본문이 아니라 multipart 로 들어오므로 인자로 받는다 */
        public MovieCommand.Create toCommand(MultipartFile thumbnail) {
            return new MovieCommand.Create(
                    title, summary, description, runningTime, ageRating,
                    releaseDate, closeDate, status, category, thumbnail
            );
        }
    }

    public record Modify(
            @NotBlank(message = "title은(는) 필수 입력 항목입니다.")
            String title,

            @NotBlank(message = "summary은(는) 필수 입력 항목입니다.")
            String summary,

            String description,

            @NotNull(message = "runningTime은(는) 필수 입력 항목입니다.")
            @Min(message = "상영시간은 0보다 작을 수 없습니다.", value = 0)
            Integer runningTime,

            @NotNull(message = "ageRating은(는) 필수 입력 항목입니다.")
            MovieAgeRating ageRating,

            @NotNull(message = "releaseDate은(는) 필수 입력 항목입니다.")
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate releaseDate,

            @NotNull(message = "closeDate은(는) 필수 입력 항목입니다.")
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate closeDate,

            @NotNull(message = "status은(는) 필수 입력 항목입니다.")
            MovieStatus status,

            @NotNull(message = "category은(는) 필수 입력 항목입니다.")
            MovieCategory category
    ) {
        /** movieId 는 PathVariable 로 들어오므로 인자로 받는다 */
        public MovieCommand.Update toCommand(Long movieId) {
            return new MovieCommand.Update(
                    movieId, title, summary, description, runningTime,
                    ageRating, releaseDate, closeDate, status, category
            );
        }
    }
}
