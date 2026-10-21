package com.example.cinema_middleware.v1.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class ScreeningRepositoryImplTest {

    @Autowired ScreeningRepository repository;

    @Test
    void test1() {
        LocalDateTime now = LocalDateTime.now();

        boolean b = repository.existsAlreadyUsing(1L, now.toLocalDate(), now.toLocalTime());

        log.info("b = {}", b);
    }

    @Test
    void test2() {
        repository.findScreeningListAtTargetDate(LocalDate.now(), null);
    }

    @Test
    void test3() {
        repository.findDetailById(1L);
    }
}