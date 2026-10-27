package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.MovieReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReservationSeatRepository extends JpaRepository<MovieReservationSeat, Long>, ReservationSeatRepositoryQuery {

    @Modifying
    @Query("UPDATE MovieReservationSeat ms SET ms.reservation_id = :reservationId WHERE ms.id IN targetIdList")
    int putReservationAll(@Param("targetIdList") List<Long> targetIdList, @Param("reservationId") Long reservationId);
}
