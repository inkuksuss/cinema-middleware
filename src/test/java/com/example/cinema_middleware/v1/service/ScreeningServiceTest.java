package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.Theater;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.repository.MovieRepository;
import com.example.cinema_middleware.v1.repository.ScreeningRepository;
import com.example.cinema_middleware.v1.repository.TheaterRepository;
import com.example.cinema_middleware.v1.service.dto.ScreeningSummary;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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

    @Test
    @DisplayName("getScreeningSummaryPageByDate 성공")
    void getScreeningSummaryByDate() {
        //given
        LocalDateTime now = LocalDateTime.now();
        Theater theater = new Theater("상영관1", "");
        theaterRepository.save(theater);
        Movie movie = new Movie("movie", "sum", 120, "19", now.toLocalDate(), now.plusDays(7).toLocalDate(), MovieCategory.THRILLER);
        movieRepository.save(movie);

        List<Screening> list = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            list.add(Screening.of(movie, theater, now.toLocalDate(), LocalTime.of(i, 0), ScreeningStatus.ON_SALE));
        }
        screeningRepository.saveAll(list);

        //when
        List<ScreeningSummary> result = screeningService.getScreeningSummaryByDate(now.toLocalDate(), null);

        //then
        Assertions.assertThat(result.size()).isEqualTo(15);
    }
}