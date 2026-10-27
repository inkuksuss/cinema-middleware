package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.repository.dao.SeatDao;

import java.util.List;

public interface TheaterSeatRepositoryQuery {

    List<SeatDao.SeatCounter> findSeatCounterListByIdList(List<Long> screeningIdList);

    List<SeatDao.Info> findAllOfScreening(Long screeningId);

    List<SeatDao.Info> findIsAvailableSeat(Long screeningId, List<Long> theaterSeatIdList);
}
