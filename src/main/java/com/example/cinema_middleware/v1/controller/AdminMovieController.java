package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.request.CreateMovieRequest;
import com.example.cinema_middleware.v1.controller.request.ModifyMovieRequest;
import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieStatus;
import com.example.cinema_middleware.v1.service.AdminMovieService;
import com.example.cinema_middleware.v1.service.dto.*;
import com.example.cinema_middleware.v1.service.dto.forms.AddMovieForm;
import com.example.cinema_middleware.v1.service.dto.forms.UpdateMovieForm;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/movie")
@RequiredArgsConstructor
public class AdminMovieController {

    private final AdminMovieService movieService;

    @GetMapping("/")
    public ResponseEntity<Result<Page<MovieDto.Summary>>> getMovieSummary(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 7) Pageable pageable,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) MovieStatus status
            ) {
        Page<MovieDto.Summary> result = movieService.getMovieSummary(pageable, title, status);

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess(result));
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<Result<MovieDto.Detail>> getMovieDetail(@PathVariable Long movieId) {
        MovieDto.Detail result = movieService.getMovieDetail(movieId);

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess(result));
    }

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Result<Void>> createMovie(
            @RequestPart MultipartFile file,
            @RequestPart @Validated CreateMovieRequest request
    ) {
        AddMovieForm form = request.toForm();
        form.setThumbnail(file);

        movieService.addMovie(form);

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess());
    }

    @PutMapping("/{movieId}")
    public ResponseEntity<Result<Void>> modifyMovie(
            @PathVariable Long movieId,
            @RequestBody @Validated ModifyMovieRequest request
    ) {
        UpdateMovieForm form = request.toForm();
        form.setMovieId(movieId);

        movieService.update(form);

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess());
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Result<Void>> deleteMovie(@PathVariable Long movieId) {
        movieService.remove(movieId);

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess());
    }
}
