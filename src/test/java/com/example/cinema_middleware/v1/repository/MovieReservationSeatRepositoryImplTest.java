package com.example.cinema_middleware.v1.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


@Transactional
@SpringBootTest
class MovieReservationSeatRepositoryImplTest {

    @Autowired MovieReservationSeatRepository repository;

    @Test
    void test() {
        repository.existsReservedSeat(1L);
    }

    @Test
    void test2() {
    }
}