package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.repository.MovieReservationSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieReservationSeatService {

    private final MovieReservationSeatRepository movieReservationSeatRepository;

    public void addReservationSeat() {

    }
}
