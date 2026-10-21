package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.UsageType;
import com.example.cinema_middleware.v1.domain.entity.enums.TableType;
import com.example.cinema_middleware.v1.repository.MovieRepository;
import com.example.cinema_middleware.v1.service.dto.*;
import com.example.cinema_middleware.v1.service.dto.forms.AddFileForm;
import com.example.cinema_middleware.v1.support.exception.TargetNotFoundException;
import jakarta.annotation.Nullable;
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

    public Page<MovieDto.Summary> getMovieSummary(Pageable pageable, @Nullable String title, @Nullable MovieStatus status) {
        Page<Movie> moviePage = movieRepository.findMoviePage(pageable, title, status);

        return moviePage.map(MovieDto.Summary::from);
    }

    public MovieDto.Detail getMovieDetail(Long movieId) {
        if (movieId == null) throw new IllegalArgumentException();

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> { throw new TargetNotFoundException(); });

        return MovieDto.Detail.from(movie);
    }

    @Transactional
    public void addMovie(MovieCommand.Create command) {
        Movie savedMovie = movieRepository.save(Movie.create(
                command.title(),
                command.summary(),
                command.description(),
                command.runningTime(),
                command.ageRating(),
                command.releaseDate(),
                command.closeDate(),
                command.status(),
                command.category()
        ));

        AddFileForm addFileForm = new AddFileForm(savedMovie.getId(), TableType.MOVIE, UsageType.THUMBNAIL, command.thumbnail());
        String destination = fileService.append(addFileForm);

        savedMovie.setPosterUrl(destination);
    }

    @Transactional
    public void update(MovieCommand.Update command) {
        Movie findMovie = movieRepository.findById(command.movieId())
                .orElseThrow(() -> { throw new TargetNotFoundException(); });

        findMovie.setTitle(command.title());
        findMovie.setSummary(command.summary());
        findMovie.setDescription(command.description());
        findMovie.setRunningTime(command.runningTime());
        findMovie.setAgeRating(command.ageRating());
        findMovie.setReleaseDate(command.releaseDate());
        findMovie.setCloseDate(command.closeDate());
        findMovie.setStatus(command.status());
        findMovie.setCategory(command.category());
    }

    public void remove(Long movieId) {
        movieRepository.deleteById(movieId);
    }
}
