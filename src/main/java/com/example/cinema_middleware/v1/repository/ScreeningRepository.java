package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ScreeningRepository extends JpaRepository<Screening, Long>, ScreeningRepositoryQuery {


    @Query("SELECT s FROM Screening s " +
            "LEFT JOIN FETCH s.movie " +
            "LEFT JOIN FETCH s.theater " +
            "WHERE s.id = :screeningId")
    Optional<Screening> findDetailById(@Param("screeningId") Long screeningId);
}
