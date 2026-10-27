package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.repository.ReservationSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieReservationSeatService {

    private final ReservationSeatRepository reservationSeatRepository;


}
