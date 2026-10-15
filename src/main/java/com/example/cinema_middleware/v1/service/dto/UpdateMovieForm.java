package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.controller.request.ModifyMovieRequest;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.YesOrNo;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@ToString
public class UpdateMovieForm {

    private Long movieId;

    private String title;

    private String summary;

    private String description;

    private Integer runningTime;

    private String ageRating;

    private LocalDate releaseDate;

    private LocalDate closeDate;

    private YesOrNo isActive;

    private MovieCategory category;

    public static UpdateMovieForm from(Long movieId, ModifyMovieRequest request) {
        UpdateMovieForm form = new UpdateMovieForm();
        form.movieId = movieId;
        form.title = request.title();
        form.summary = request.summary();
        form.description = request.description();
        form.runningTime = request.runningTime();
        form.ageRating = request.ageRating();
        form.releaseDate = request.releaseDate();
        form.closeDate = request.closeDate();
        form.isActive = request.isActive();
        form.category = request.category();

        return form;
    }
}
