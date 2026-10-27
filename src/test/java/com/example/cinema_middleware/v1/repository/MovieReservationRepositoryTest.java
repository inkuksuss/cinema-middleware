package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Transactional
@SpringBootTest
class MovieReservationRepositoryTest {

    @Autowired
    MovieReservationRepository movieReservationRepository;

}