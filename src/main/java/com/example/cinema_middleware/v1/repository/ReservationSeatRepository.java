package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.MovieReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationSeatRepository extends JpaRepository<MovieReservationSeat, Long>, ReservationSeatRepositoryQuery {
}
