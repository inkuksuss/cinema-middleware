package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long>, MovieRepositoryQuery {
}
