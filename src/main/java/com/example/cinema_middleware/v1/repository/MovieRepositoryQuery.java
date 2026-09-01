package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDate;

public interface MovieRepositoryQuery {

    Page<Movie> findSummaryWithPage(Pageable pageable, LocalDate targetDate);

    Slice<Movie> findSummaryWithSlice(Pageable pageable, LocalDate targetDate);
}
