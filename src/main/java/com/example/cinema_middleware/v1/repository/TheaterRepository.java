package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Theater;
import com.example.cinema_middleware.v1.domain.entity.TheaterSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TheaterRepository extends JpaRepository<Theater, Long>, TheaterRepositoryQuery {

}
