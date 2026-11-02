package com.example.cinema_middleware.v1.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CinemaRepositoryTest {

    @Autowired
    CinemaRepository cinemaRepository;

    @Test
    void existsDuplicateTheaterName() {
        cinemaRepository.existsDuplicateTheaterName(1L, "hello");
    }

}