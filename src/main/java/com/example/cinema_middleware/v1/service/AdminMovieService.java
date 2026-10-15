package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.enums.UsageType;
import com.example.cinema_middleware.v1.domain.entity.enums.TableType;
import com.example.cinema_middleware.v1.domain.entity.enums.YesOrNo;
import com.example.cinema_middleware.v1.repository.MovieRepository;
import com.example.cinema_middleware.v1.service.dto.*;
import com.example.cinema_middleware.v1.support.exception.TargetNotFoundException;
import jakarta.annotation.Nullable;
import org.hibernate.ObjectNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMovieService {

    private final MovieRepository movieRepository;
    private final FileService fileService;

    public AdminMovieService(MovieRepository movieRepository, FileService fileService) {
        this.movieRepository = movieRepository;
        this.fileService = fileService;
    }

    public Page<MovieSummary> getMovieSummary(Pageable pageable, @Nullable String title, @Nullable YesOrNo isActive) {
        Page<Movie> moviePage = movieRepository.findMoviePage(pageable, title, isActive);

        return moviePage.map(MovieSummary::from);
    }

    public MovieDetail getMovieDetail(Long movieId) {
        if (movieId == null) throw new IllegalArgumentException();

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> { throw new TargetNotFoundException(); });

        return MovieDetail.from(movie);
    }

    @Transactional
    public void addMovie(AddMovieForm form) {
        Movie savedMovie = movieRepository.save(Movie.create(form));

        AddFileForm addFileForm = new AddFileForm(savedMovie.getId(), TableType.MOVIE, UsageType.THUMBNAIL, form.getThumbnail());
        String destination = fileService.append(addFileForm);

        savedMovie.setPosterUrl(destination);
    }

    @Transactional
    public void update(UpdateMovieForm form) {
        Movie findMovie = movieRepository.findById(form.getMovieId())
                .orElseThrow(() -> { throw new TargetNotFoundException(); });

        findMovie.setTitle(form.getTitle());
        findMovie.setSummary(form.getSummary());
        findMovie.setDescription(form.getDescription());
        findMovie.setRunningTime(form.getRunningTime());
        findMovie.setAgeRating(form.getAgeRating());
        findMovie.setReleaseDate(form.getReleaseDate());
        findMovie.setCloseDate(form.getCloseDate());
        findMovie.setIsActive(form.getIsActive());
        findMovie.setCategory(form.getCategory());
    }

    public void remove(Long movieId) {
        movieRepository.deleteById(movieId);
    }
}
