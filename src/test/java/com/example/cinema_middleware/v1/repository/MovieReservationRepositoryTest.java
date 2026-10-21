package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.repository.dto.search.AdminReservationSummarySearch;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
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

    @Test
    void test() {
        Optional<MovieReservation> reservationById = movieReservationRepository.findDetailById(Long.valueOf(1L));

        reservationById.get();
    }

    @Test
    void test2() {
        movieReservationRepository.existsAliveByScreeningId(9999L);
    }

    @Test
    void test3() {
        AdminReservationSummarySearch search = new AdminReservationSummarySearch(PageRequest.of(0, 7), LocalDate.now(), LocalDate.now().plusDays(7));

        movieReservationRepository.findReservationAdminRow(search);
    }

}