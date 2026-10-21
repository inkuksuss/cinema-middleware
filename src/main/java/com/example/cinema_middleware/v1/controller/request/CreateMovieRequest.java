package com.example.cinema_middleware.v1.controller.request;

import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieStatus;
import com.example.cinema_middleware.v1.service.dto.forms.AddMovieForm;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record CreateMovieRequest(
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
        public AddMovieForm toForm() {
                AddMovieForm form = new AddMovieForm();
                form.setTitle(title);
                form.setSummary(summary);
                form.setDescription(description);
                form.setRunningTime(runningTime);
                form.setAgeRating(ageRating);
                form.setReleaseDate(releaseDate);
                form.setCloseDate(closeDate);
                form.setStatus(status);
                form.setCategory(category);

                return form;
        }
}
