package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Theater;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TheaterRepositoryQuery {

    Page<Theater> findPageInCinema(Pageable pageable, Long cinemaId);
}
