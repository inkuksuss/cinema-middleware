package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.*;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import com.example.cinema_middleware.v1.repository.dto.ScreeningSeat;
import com.example.cinema_middleware.v1.repository.dto.SeatCount;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Transactional
@SpringBootTest
class ScreeningRepositoryTest {

    @Autowired
    MovieReservationSeatRepository movieReservationSeatRepository;
    @Autowired
    ScreeningRepository screeningRepository;
    @Autowired
    MovieReservationRepository movieReservationRepository;
    @Autowired
    TheaterRepository theaterRepository;
    @Autowired
    TheaterSeatRepository theaterSeatRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    MovieRepository movieRepository;

    @Test
    @DisplayName("")
    void get() {
        // given
        LocalDate now = LocalDate.now();
        Member member = Member.of(
                "ad@naver.com",
                "hello",
                "123",
                "01022223333",
                "20111111"
        );
        Member savedMember = memberRepository.save(member);
        Theater theater = new Theater("theater1", "desc");
        Theater savedTheater = theaterRepository.save(theater);
        Movie movie = new Movie("hello", "aa", 120, "19", now, now.plusDays(7), MovieCategory.ROMANCE);
        Movie savedMovie = movieRepository.save(movie);
        Screening screening = Screening.of(savedMovie, savedTheater, now, LocalTime.MIN, ScreeningStatus.ON_SALE);
        Screening savedScreening = screeningRepository.save(screening);
        MovieReservation movieReservation = MovieReservation.of(savedMember, savedScreening, List.of(BigDecimal.valueOf(10000)));
        MovieReservation savedReservation = movieReservationRepository.save(movieReservation);
        TheaterSeat theaterSeat = TheaterSeat.of(savedTheater, "1", "a", SeatGrade.NORMAL);
        TheaterSeat theaterSeat2 = TheaterSeat.of(savedTheater, "1", "b", SeatGrade.NORMAL);
        theaterSeatRepository.saveAll(List.of(theaterSeat, theaterSeat2));
        MovieReservationSeat movieReservationSeat = MovieReservationSeat.of(savedReservation, screening, theaterSeat);
        movieReservationSeatRepository.save(movieReservationSeat);
        // when
        List<SeatCount> result = screeningRepository.findSeatCountListByIdList(List.of(screening.getId()));

        // then
        Assertions.assertThat(result.get(0).getTotalSeatCount()).isEqualTo(2);
        Assertions.assertThat(result.get(0).getSoldOutSeatCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("")
    void queryTest() {
        List<ScreeningSeat> screeningSeat = screeningRepository.findAllSeatByScreeningId(1L);
        screeningSeat.forEach(v -> log.info(v.toString()));

        screeningRepository.findSeatCountListByIdList(List.of(1L));
        screeningRepository.findAllSeatByScreeningId(1L);
        screeningRepository.findSeatPriceListFor(1L, List.of(1L, 2L));
    }

}