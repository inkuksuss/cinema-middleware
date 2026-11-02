package com.example.cinema_middleware.v1.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ReservationSeatRepositoryTest {

    @Autowired
    ReservationSeatRepository reservationSeatRepository;

    @Test
    void existsReservedSeat() {
        reservationSeatRepository.existsReservedSeat(1L);
    }

}