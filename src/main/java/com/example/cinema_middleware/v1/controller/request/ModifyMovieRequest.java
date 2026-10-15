package com.example.cinema_middleware.v1.controller.request;

import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.YesOrNo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ModifyMovieRequest(
        @NotBlank(message = "title은(는) 필수 입력 항목입니다.")
        String title,
        @NotBlank(message = "summary은(는) 필수 입력 항목입니다.")
        String summary,

        String description,

        @NotNull(message = "runningTime은(는) 필수 입력 항목입니다.")
        @Min(message = "상영시간은 0보다 작을 수 없습니다.", value = 0)
        Integer runningTime,

        @NotBlank(message = "ageRating은(는) 필수 입력 항목입니다.")
        String ageRating,

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
}
