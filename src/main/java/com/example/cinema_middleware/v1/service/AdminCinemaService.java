package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Cinema;
import com.example.cinema_middleware.v1.repository.CinemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminCinemaService {

    private final CinemaRepository cinemaRepository;

    public boolean checkDuplicateName(Long cinemaId, String name) {
        return cinemaRepository.existsDuplicateTheaterName(cinemaId, name);
    }




    public Cinema getRefer(Long id) {
        return cinemaRepository.getReferenceById(id);
    }
}
