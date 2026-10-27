package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.repository.MovieRepository;
import com.example.cinema_middleware.v1.service.dto.MovieDto;
import com.example.cinema_middleware.v1.service.factory.MovieDtoFactory;
import com.example.cinema_middleware.v1.support.exception.TargetNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    public Slice<MovieDto.Summary> getMovieSummaryPage(Pageable pageable) {
        return movieRepository.findMovieSliceByDate(pageable, LocalDate.now())
                .map(MovieDtoFactory::summary);
    }


    public MovieDto.Detail getMovieDetail(Long movieId) {
        Movie findMovie = movieRepository.findById(movieId)
                .orElseThrow(() -> { throw new TargetNotFoundException(); });

        return MovieDtoFactory.detail(findMovie);
    }
}
