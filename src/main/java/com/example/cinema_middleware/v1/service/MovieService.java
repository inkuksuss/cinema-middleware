package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.repository.MovieRepository;
import com.example.cinema_middleware.v1.service.dto.MovieDetail;
import com.example.cinema_middleware.v1.service.dto.MovieSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    public Page<MovieSummary> getMovieSummaryPage(Pageable pageable) {
        return movieRepository.findMoviePageByDate(pageable, LocalDate.now())
                .map(MovieSummary::from);
    }


    public MovieDetail getMovieDetail(Long movieId) {
        Movie findMovie = movieRepository.findById(movieId)
                .orElseThrow(() -> { throw new IllegalArgumentException("잘못된 영화 ID입니다."); });

        return MovieDetail.from(findMovie);
    }
}
