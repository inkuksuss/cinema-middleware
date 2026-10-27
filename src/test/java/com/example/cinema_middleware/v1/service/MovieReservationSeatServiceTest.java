package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.*;
import com.example.cinema_middleware.v1.domain.entity.enums.*;
import com.example.cinema_middleware.v1.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@SpringBootTest
class MovieReservationSeatServiceTest {

    @Autowired
    ReservationSeatRepository movieReservationSeatRepository;
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
    @Autowired
    CinemaRepository cinemaRepository;

    @AfterEach
    void clear() {
        movieReservationSeatRepository.deleteAll();
        screeningRepository.deleteAll();
        movieReservationRepository.deleteAll();
        movieRepository.deleteAll();
        theaterSeatRepository.deleteAll();
        theaterRepository.deleteAll();
        memberRepository.deleteAll();
    }


    @Test
    @DisplayName("addReservationSeat: 중복 자리 선점 체크")
    void addReservationSeatCheckDuplicate() {
        //given
        LocalDate now = LocalDate.now();
        Member member = Member.create(
                "ad@naver.com",
                "hello",
                "123",
                "01022223333",
                "20111111",
                MemberGrade.ROLE_COMMON,
                SocialProvider.NORMAL
        );
        Member savedMember = memberRepository.save(member);
        Theater theater = Theater.create(cinemaRepository.getReferenceById(1L), "theater1", "desc");
        Theater savedTheater = theaterRepository.save(theater);
        Movie movie = new Movie();
        movie.setTitle("hello");
        movie.setSummary("aa");
        movie.setRunningTime(120);
        movie.setAgeRating(MovieAgeRating.ONLY_ADULT);
        movie.setReleaseDate(now);
        movie.setCloseDate(now.plusDays(7));
        movie.setCategory(MovieCategory.ROMANCE);
        movie.setStatus(MovieStatus.DISPLAY);
        Movie savedMovie = movieRepository.save(movie);
        Screening screening = Screening.create(savedMovie, savedTheater, now, LocalTime.MIN, ScreeningStatus.ON_SALE);
        Screening savedScreening = screeningRepository.save(screening);
        MovieReservation movieReservation = MovieReservation.create(savedMember, savedScreening, "a", "a", BigDecimal.TEN);
        MovieReservation savedReservation = movieReservationRepository.save(movieReservation);
        TheaterSeat theaterSeat = TheaterSeat.create(savedTheater, "1", "a", SeatGrade.NORMAL);
        TheaterSeat savedTheaterSeat = theaterSeatRepository.save(theaterSeat);

        ExecutorService es = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(10);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        //when
        for (int i = 0; i < 10; i++) {
            es.submit(() -> {
                try {
                    MovieReservationSeat movieReservationSeat = MovieReservationSeat.createTemp(savedScreening, savedTheaterSeat);
                    startLatch.await();
                    movieReservationSeatRepository.save(movieReservationSeat);
                }
                catch (DataIntegrityViolationException e) {
                    duplicateCount.incrementAndGet();
                }
                catch (Exception e) {
                    log.info(e.getMessage());
                }
                finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        try {
            doneLatch.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.info(e.getMessage());
        } finally {
            es.shutdown();
        }

        //then
        Assertions.assertThat(duplicateCount.get()).isEqualTo(9);
    }

    @Test
    @DisplayName("addReservationSeat: 중복 자리 선점 null 체크")
    void addReservationSeatCheckDuplicateNull() {
        //given
        LocalDate now = LocalDate.now();
        Member member = Member.create(
                "ad@naver.com",
                "hello",
                "123",
                "01022223333",
                "20111111",
                MemberGrade.ROLE_COMMON,
                SocialProvider.NORMAL
        );
        Member savedMember = memberRepository.save(member);
        Theater theater = Theater.create(cinemaRepository.getReferenceById(1L), "theater1", "desc");
        Theater savedTheater = theaterRepository.save(theater);
        Movie movie = new Movie();
        movie.setTitle("hello");
        movie.setSummary("aa");
        movie.setRunningTime(120);
        movie.setAgeRating(MovieAgeRating.ONLY_ADULT);
        movie.setReleaseDate(now);
        movie.setCloseDate(now.plusDays(7));
        movie.setCategory(MovieCategory.ROMANCE);
        movie.setStatus(MovieStatus.DISPLAY);
        Movie savedMovie = movieRepository.save(movie);
        Screening screening = Screening.create(savedMovie, savedTheater, now, LocalTime.MIN, ScreeningStatus.ON_SALE);
        Screening savedScreening = screeningRepository.save(screening);
        MovieReservation movieReservation = MovieReservation.create(savedMember, savedScreening, "a", "b", BigDecimal.TEN);
        MovieReservation savedReservation = movieReservationRepository.save(movieReservation);
        TheaterSeat theaterSeat = TheaterSeat.create(savedTheater, "1", "a", SeatGrade.NORMAL);
        TheaterSeat savedTheaterSeat = theaterSeatRepository.save(theaterSeat);

        ExecutorService es = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(10);
        AtomicInteger saveCount = new AtomicInteger(0);

        //when
        for (int i = 0; i < 10; i++) {
            es.submit(() -> {
                try {
                    MovieReservationSeat movieReservationSeat = MovieReservationSeat.createTemp(savedScreening, savedTheaterSeat);
                    movieReservationSeat.giveUpSeat();
                    startLatch.await();
                    movieReservationSeatRepository.save(movieReservationSeat);
                    saveCount.incrementAndGet();
                }
                catch (Exception e) {
                    log.info(e.getMessage());
                }
                finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        try {
            doneLatch.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.info(e.getMessage());
        } finally {
            es.shutdown();
        }

        //then
        Assertions.assertThat(saveCount.get()).isEqualTo(10);
    }
}