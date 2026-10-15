package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.controller.request.CreateMovieRequest;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.YesOrNo;
import lombok.Getter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@ToString
public class AddMovieForm {

    private String title;

    private String summary;

    private String description;

    private Integer runningTime;

    private String ageRating;

    private LocalDate releaseDate;

    private LocalDate closeDate;

    private YesOrNo isActive;

    private MovieCategory category;

    private MultipartFile thumbnail;

    public static AddMovieForm from(CreateMovieRequest request, MultipartFile file) {
        AddMovieForm form = new AddMovieForm();
        form.title = request.title();
        form.summary = request.summary();
        form.description = request.description();
        form.runningTime = request.runningTime();
        form.ageRating = request.ageRating();
        form.releaseDate = request.releaseDate();
        form.closeDate = request.closeDate();
        form.isActive = request.isActive();
        form.category = request.category();
        form.thumbnail = file;

        return form;
    }
}
