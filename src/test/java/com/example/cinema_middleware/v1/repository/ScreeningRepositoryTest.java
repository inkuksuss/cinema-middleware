package com.example.cinema_middleware.v1.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ScreeningRepositoryTest {

    @Autowired
    ScreeningRepository screeningRepository;

    @Test
    void findPreviewAtTargetDate() {
        screeningRepository.findPreviewAtTargetDate(LocalDate.now(), 1L, 1L);
    }

    @Test
    void findAdminRow() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        screeningRepository.findAdminRow(pageRequest, LocalDate.now(), 1L, 1L);
    }

    @Test
    void findAdminDetail() {

    }

}