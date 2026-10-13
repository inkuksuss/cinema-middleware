package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MovieReservationRepository extends JpaRepository<MovieReservation, Long>, MovieReservationRepositoryQuery {

    @Query("SELECT mr FROM MovieReservation mr " +
            "JOIN FETCH mr.member " +
            "JOIN FETCH mr.payment " +
            "WHERE mr.id = :reservationId")
    Optional<MovieReservation> findDetailById(@Param("reservationId") Long reservationId);
}
