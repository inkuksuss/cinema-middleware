package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.MovieReservationSeat;
import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.repository.ReservationSeatRepository;
import com.example.cinema_middleware.v1.repository.TheaterSeatRepository;
import com.example.cinema_middleware.v1.repository.dao.SeatDao;
import com.example.cinema_middleware.v1.support.exception.ScreeningNotOnSaleException;
import com.example.cinema_middleware.v1.support.exception.SeatAlreadyTakenException;
import com.example.cinema_middleware.v1.support.exception.TargetNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatService {

    private final TheaterSeatRepository theaterSeatRepository;
    private final ReservationSeatRepository reservationSeatRepository;


    public List<SeatDao.Info> getAllOfScreening(Long screeningId) {
        return theaterSeatRepository.findAllOfScreening(screeningId);
    }

    public List<SeatDao.Info> checkIsAvailable(Long screeningId, List<Long> seatIdList) {
        List<SeatDao.Info> isAvailableSeat = theaterSeatRepository.findIsAvailableSeat(screeningId, seatIdList);

        if (isAvailableSeat.size() == 0 || isAvailableSeat.size() != seatIdList.size()) {
            throw new TargetNotFoundException("좌석 정보를 찾을 수 없습니다.");
        }

        if (!ScreeningStatus.ON_SALE.equals(isAvailableSeat.getFirst().screeningStatus())) {
            throw new ScreeningNotOnSaleException();
        }

        return isAvailableSeat;
    }

    @Transactional
    public List<MovieReservationSeat> putIfNotTaken(List<Long> seatIdList, Screening screening) {
        List<MovieReservationSeat> reservationSeatList = seatIdList.stream()
                .map(theaterSeatRepository::getReferenceById)
                .map(seatRef -> MovieReservationSeat.createTemp(screening, seatRef))
                .toList();

        try {
            reservationSeatRepository.saveAllAndFlush(reservationSeatList);
        }
        catch (DataIntegrityViolationException e) {
            throw new SeatAlreadyTakenException("이미 예매된 좌석입니다.");
        }

        return reservationSeatList;
    }

    @Transactional
    public void putReservation(List<Long> targetIdList, Long reservationId) {
        Assert.notEmpty(targetIdList, "targetIdList can not be empty");
        Assert.notNull(reservationId, "reservationId can not be null");

        int result = reservationSeatRepository.putReservationAll(targetIdList, reservationId);

        if (result == 0 || result != targetIdList.size()) {
            throw new IllegalStateException();
        }
    }
}
