package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CinemaRepository extends JpaRepository<Cinema, Long>, CinemaRepositoryQuery {
}
