package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieReservationRepository extends JpaRepository<MovieReservation, Long> {

}
