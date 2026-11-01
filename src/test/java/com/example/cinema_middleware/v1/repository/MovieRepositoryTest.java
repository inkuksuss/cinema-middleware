package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.enums.MovieStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MovieRepositoryTest {

    @Autowired
    MovieRepository movieRepository;


    @Test
    void findMovieSliceByDate() {
        PageRequest pageRequest = PageRequest.of(0, 1);
        movieRepository.findMovieSliceByDate(pageRequest, LocalDate.now());


    }

    @Test
    void findMoviePage() {
        PageRequest pageRequest = PageRequest.of(0, 1);
        movieRepository.findMoviePage(pageRequest, "asad", MovieStatus.DISPLAY);
    }


}