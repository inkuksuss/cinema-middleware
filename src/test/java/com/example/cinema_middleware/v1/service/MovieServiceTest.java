package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.repository.MovieRepository;
import com.example.cinema_middleware.v1.service.dto.MovieSummaryDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class MovieServiceTest {

    @Autowired
    MovieRepository movieRepository;
    @Autowired
    MovieService movieService;

    @AfterEach
    void clear() {
        movieRepository.deleteAll();
    }

    @Test
    @DisplayName("getMovieSummaryPage: 정상 조회")
    void getMovieSummaryPage() {
        //given
        List<Movie> movies = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 100; i++) {
            movies.add(new Movie(
                    "영화" + i,
                    "영화" + i + " 요약이다.",
                    120,
                    "15세",
                    now.minusDays(0),
                    now.plusDays(7),
                    i % 2 == 0 ? MovieCategory.THRILLER : MovieCategory.ROMANCE
                    ));
        }
        movieRepository.saveAll(movies);

        //when
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<MovieSummaryDto> movieSummary = movieService.getMovieSummaryPage(pageRequest);

        //then
        assertThat(movieSummary).hasSize(10);
    }

    @Test
    @DisplayName("getMovieSummaryPage: 예매 기간 밖")
    void getMovieSummaryPageInvalidDate() {
        //given
        List<Movie> movies = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 100; i++) {
            movies.add(new Movie(
                    "영화" + i,
                    "영화" + i + " 요약이다.",
                    120,
                    "15세",
                    now.plusDays(7),
                    now.plusDays(7),
                    i % 2 == 0 ? MovieCategory.THRILLER : MovieCategory.ROMANCE
            ));
        }
        movieRepository.saveAll(movies);

        //when
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<MovieSummaryDto> movieSummary = movieService.getMovieSummaryPage(pageRequest);

        //then
        assertThat(movieSummary).hasSize(0);
    }

}