package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.TheaterSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TheaterSeatRepository extends JpaRepository<TheaterSeat, Long>, TheaterSeatRepositoryQuery {

    @Query("SELECT ts FROM TheaterSeat ts " +
            "JOIN FETCH ts.theater t " +
            "WHERE t.id = :theaterId " +
            "ORDER BY ts.seatRow ASC, ts.seatColumn ASC")
    List<TheaterSeat> findAllByTheaterId(@Param(value = "theaterId") Long theaterId);
}
