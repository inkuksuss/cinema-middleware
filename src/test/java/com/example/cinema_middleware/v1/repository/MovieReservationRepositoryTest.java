package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.repository.dao.search.ReservationCondition;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;


@Slf4j
@Transactional
@SpringBootTest
class MovieReservationRepositoryTest {

    @Autowired
    MovieReservationRepository reservationRepository;

    @Test
    void findMyReservationPage() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        reservationRepository.findMyReservationPage(pageRequest, 1L, ReservationStatus.SUCCESS);
    }

    @Test
    void findMyReservationSlice() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        reservationRepository.findMyReservationSlice(pageRequest, 1L, ReservationStatus.SUCCESS);
    }

    @Test
    void findMyReservationDetail() {
        reservationRepository.findMyReservationDetail(1L);
    }

    @Test
    void findAdminRow() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        LocalDate now = LocalDate.now();
        reservationRepository.findAdminRow(new ReservationCondition.AdminRow(pageRequest, now, now.plusDays(7), ReservationStatus.SUCCESS, null));
    }

    @Test
    void findAdminDetail() {
        reservationRepository.findAdminDetail(1L);
    }

    @Test
    void existsAliveByScreeningId() {
        reservationRepository.existsAliveByScreeningId(1L);
    }

}