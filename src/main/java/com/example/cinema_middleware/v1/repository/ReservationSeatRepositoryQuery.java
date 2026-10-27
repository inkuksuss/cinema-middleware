package com.example.cinema_middleware.v1.repository;

public interface ReservationSeatRepositoryQuery {

    boolean existsReservedSeat(Long theaterSeatId);
}
