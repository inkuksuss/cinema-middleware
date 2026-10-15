package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Theater;
import com.example.cinema_middleware.v1.repository.TheaterRepository;
import com.example.cinema_middleware.v1.service.dto.TheaterSummary;
import com.example.cinema_middleware.v1.support.exception.DuplicateValueException;
import com.example.cinema_middleware.v1.support.exception.TargetNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AdminTheaterService {

    private final TheaterRepository theaterRepository;

    public Page<TheaterSummary> getTheaterSummary(Pageable pageable) {
        return theaterRepository.findTheaterPage(pageable)
                .map(t -> TheaterSummary.from(t));
    }

    public void addTheater(String name, String description) {
        theaterRepository.findByName(name)
                         .ifPresent(t -> { throw new DuplicateValueException("이미 존재하는 상영관 명입니다."); });

        theaterRepository.save(Theater.create(name, description));
    }

    @Transactional
    public void update(Long theaterId, String name, String description) {
        Theater findTheater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> { throw new TargetNotFoundException(); });

        findTheater.setName(name);
        findTheater.setDescription(description);
    }

    public void remove(Long theaterId) {
        theaterRepository.deleteById(theaterId);
    }
}
