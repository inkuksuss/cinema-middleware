package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.service.MovieService;
import com.example.cinema_middleware.v1.service.dto.MovieDetailDto;
import com.example.cinema_middleware.v1.service.dto.MovieSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController("/api/v1/movie")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/summary")
    public ResponseEntity<Result<Page<MovieSummaryDto>>> getSummary(@PageableDefault(sort = "created", direction = Sort.Direction.DESC, size = 7) Pageable pageable) {
        //TODO
        // front 페이지는 1번 시작이지만, 조회는 0번부터 시작
        Page<MovieSummaryDto> movieSummary = movieService.getMovieSummaryPage(pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Result.ofSuccess(movieSummary));
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<Result<MovieDetailDto>> getDetail(@PathVariable Long movieId) {
        if (movieId == null) {
            throw new IllegalArgumentException("잘못된 영화 ID입니다.");
        }

        MovieDetailDto movieDetail = movieService.getMovieDetail(movieId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Result.ofSuccess(movieDetail));
    }
}
