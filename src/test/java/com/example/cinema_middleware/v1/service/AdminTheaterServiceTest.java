package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Theater;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatStatus;
import com.example.cinema_middleware.v1.repository.CinemaRepository;
import com.example.cinema_middleware.v1.repository.TheaterRepository;
import com.example.cinema_middleware.v1.service.dto.SeatCommand;
import com.example.cinema_middleware.v1.service.dto.TheaterCommand;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Transactional
@SpringBootTest
class AdminTheaterServiceTest {

    @Autowired AdminSeatService seatService;
    @Autowired TheaterRepository theaterRepository;
    @Autowired
    CinemaRepository cinemaRepository;


    @Test
    void bulkAddUpper() {
        //given
        final int size = 201;
        Theater savedTheater = theaterRepository.save(Theater.create(cinemaRepository.getReferenceById(1L),"test1", "1"));

        List<SeatCommand.Create> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(new SeatCommand.Create("A", "B", SeatGrade.NORMAL, BigDecimal.ONE, SeatStatus.AVAILABLE));
        }

        //when
        seatService.saveAll(savedTheater, list);

        //then
        Assertions.assertThat(seatService.getAllByTheaterId(savedTheater.getId()))
                .hasSize(size);
    }

    @Test
    void bulkAddEqual() {
        //given
        final int size = 200;
        Theater savedTheater = theaterRepository.save(Theater.create(cinemaRepository.getReferenceById(1L),"test1", "1"));

        List<SeatCommand.Create> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(new SeatCommand.Create("A", "B", SeatGrade.NORMAL, BigDecimal.ONE, SeatStatus.AVAILABLE));
        }

        //when
        seatService.saveAll(savedTheater, list);

        //then
        Assertions.assertThat(seatService.getAllByTheaterId(savedTheater.getId()))
                .hasSize(size);
    }

    @Test
    void bulkAddUnder() {
        //given
        final int size = 199;
        Theater savedTheater = theaterRepository.save(Theater.create(cinemaRepository.getReferenceById(1L),"test1", "1"));

        List<SeatCommand.Create> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(new SeatCommand.Create("A", "B", SeatGrade.NORMAL, BigDecimal.ONE, SeatStatus.AVAILABLE));
        }

        //when
        seatService.saveAll(savedTheater, list);

        //then
        Assertions.assertThat(seatService.getAllByTheaterId(savedTheater.getId()))
                .hasSize(size);
    }
}