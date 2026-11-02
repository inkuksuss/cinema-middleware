package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MovieReservationRepository extends JpaRepository<MovieReservation, UUID>, MovieReservationRepositoryQuery {

}
