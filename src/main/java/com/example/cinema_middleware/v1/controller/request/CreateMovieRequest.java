package com.example.cinema_middleware.v1.controller.request;

import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.YesOrNo;
import com.example.cinema_middleware.v1.service.dto.AddMovieForm;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public record CreateMovieRequest(
        @NotBlank(message = "title은(는) 필수 입력 항목입니다.")
        String title,
        @NotBlank(message = "summary은(는) 필수 입력 항목입니다.")
        String summary,

        String description,

        @NotNull(message = "runningTime은(는) 필수 입력 항목입니다.")
        @Min(message = "상영시간은 0보다 작을 수 없습니다.", value = 0)
        Integer runningTime,

        @NotBlank(message = "ageRating은(는) 필수 입력 항목입니다.")
        MovieAgeRating ageRating,

        @NotNull(message = "releaseDate은(는) 필수 입력 항목입니다.")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate releaseDate,

        @NotNull(message = "closeDate은(는) 필수 입력 항목입니다.")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate closeDate,

        @NotBlank(message = "isActive은(는) 필수 입력 항목입니다.")
        YesOrNo isActive,

        @NotBlank(message = "category은(는) 필수 입력 항목입니다.")
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
                form.setIsActive(isActive);
                form.setCategory(category);

                return form;
        }
}
