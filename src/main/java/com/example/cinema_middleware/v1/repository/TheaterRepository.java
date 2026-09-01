package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheaterRepository extends JpaRepository<Theater, Long> {
}
