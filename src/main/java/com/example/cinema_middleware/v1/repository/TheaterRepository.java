package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Theater;
import com.example.cinema_middleware.v1.domain.entity.TheaterSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TheaterRepository extends JpaRepository<Theater, Long> {

    @Query("SELECT ts FROM TheaterSeat ts JOIN FETCH ts.movieReservationSeatList ")
    public List<TheaterSeat> getSeatListByScreeningIdList(List<Long> screeningList);
}
