package com.example.cinema_middleware.v1.service.dto.forms;

import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter @Setter
@ToString
public class AddMovieForm {

    private String title;

    private String summary;

    private String description;

    private Integer runningTime;

    private MovieAgeRating ageRating;

    private LocalDate releaseDate;

    private LocalDate closeDate;

    private MovieStatus status;

    private MovieCategory category;

    private MultipartFile thumbnail;
}
