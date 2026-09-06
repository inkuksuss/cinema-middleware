package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.repository.TheaterSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TheaterSeatService {

    private final TheaterSeatRepository theaterSeatRepository;
}
