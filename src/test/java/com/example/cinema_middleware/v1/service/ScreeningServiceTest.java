package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.enums.ScreenType;
import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.Theater;
import com.example.cinema_middleware.v1.domain.entity.TheaterSeat;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.repository.CinemaRepository;
import com.example.cinema_middleware.v1.repository.MovieRepository;
import com.example.cinema_middleware.v1.repository.ScreeningRepository;
import com.example.cinema_middleware.v1.repository.TheaterRepository;
import com.example.cinema_middleware.v1.repository.TheaterSeatRepository;
import com.example.cinema_middleware.v1.service.dto.ScreeningDto;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Transactional
@SpringBootTest
class ScreeningServiceTest {

    @Autowired
    ScreeningRepository screeningRepository;
    @Autowired
    TheaterRepository theaterRepository;
    @Autowired
    MovieRepository movieRepository;
    @Autowired
    ScreeningService screeningService;

    @Autowired
    CinemaRepository cinemaRepository;
    @Autowired
    TheaterSeatRepository theaterSeatRepository;

    @Test
    @DisplayName("getScreeningSummaryPageByDate 성공")
    void getSummaryByDate() {
        //given
        LocalDateTime now = LocalDateTime.now();
        Theater theater = Theater.create(cinemaRepository.getReferenceById(1L), "상영관1", "테스트 상영관");
        theaterRepository.save(theater);
        // 회차 요약은 상영관 좌석과 내부 조인하므로 좌석이 하나 이상 있어야 조회된다
        theaterSeatRepository.save(TheaterSeat.create(theater, "A", "1", SeatGrade.NORMAL, BigDecimal.valueOf(SeatGrade.NORMAL.getPrice())));
        Movie movie = new Movie();
        movie.setTitle("hello");
        movie.setSummary("aa");
        movie.setRunningTime(120);
        movie.setAgeRating(MovieAgeRating.ONLY_ADULT);
        movie.setReleaseDate(now.toLocalDate());
        movie.setCloseDate(now.toLocalDate().plusDays(7));
        movie.setCategory(MovieCategory.ROMANCE);
        movie.setStatus(MovieStatus.DISPLAY);
        movieRepository.save(movie);

        List<Screening> list = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            list.add(Screening.create(movie, theater, ScreenType.V_2D, now.toLocalDate(), LocalTime.of(i, 0), ScreeningStatus.ON_SALE));
        }
        screeningRepository.saveAll(list);

        //when
        // 적재된 테스트 데이터의 회차와 섞이지 않도록 이 테스트에서 만든 영화로 한정한다
        List<ScreeningDto.Summary> result = screeningService.getSummaryByDate(now.toLocalDate(), 1L, movie.getId());

        //then
        Assertions.assertThat(result.size()).isEqualTo(15);
    }
}