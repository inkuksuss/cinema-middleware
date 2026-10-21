package com.example.cinema_middleware.v1.repository;

public interface MovieReservationSeatRepositoryQuery {

    boolean existsReservedSeat(Long theaterSeatId);
}
