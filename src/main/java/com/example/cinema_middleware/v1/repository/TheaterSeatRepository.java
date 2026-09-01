package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.TheaterSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheaterSeatRepository extends JpaRepository<TheaterSeat, Long> {
}
