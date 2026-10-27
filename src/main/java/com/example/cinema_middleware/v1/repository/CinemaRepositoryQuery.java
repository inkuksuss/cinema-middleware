package com.example.cinema_middleware.v1.repository;

public interface CinemaRepositoryQuery {

    boolean existsDuplicateTheaterName(Long cinemaId, String name);
}
