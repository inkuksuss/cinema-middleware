package com.example.cinema_middleware.v1.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TheaterRepositoryImplTest {

    @Autowired TheaterRepository theaterRepository;

    @Test
    void test() {
        theaterRepository.findTheaterPage(PageRequest.of(0, 7, Sort.by("createdAt")));
    }

}