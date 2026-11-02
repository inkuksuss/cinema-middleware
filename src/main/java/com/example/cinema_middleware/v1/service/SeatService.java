package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.domain.entity.MovieReservationSeat;
import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatStatus;
import com.example.cinema_middleware.v1.repository.ReservationSeatRepository;
import com.example.cinema_middleware.v1.repository.TheaterSeatRepository;
import com.example.cinema_middleware.v1.repository.dao.SeatDao;
import com.example.cinema_middleware.v1.support.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
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
        Assert.notEmpty(seatIdList, "seatIdList can not be empty");

        // 같은 좌석이 중복 요청되면 IN 조회 결과 행 수가 줄어 '좌석 없음'으로 오진단되므로 입력 오류로 먼저 거른다
        if (seatIdList.stream().distinct().count() != seatIdList.size()) {
            throw new DuplicateSeatException("중복된 좌석이 포함되어 있습니다.");
        }

        List<SeatDao.Info> isAvailableSeat = theaterSeatRepository.findIsAvailableSeat(screeningId, seatIdList);

        if (isAvailableSeat.isEmpty() || isAvailableSeat.size() != seatIdList.size()) {
            throw new TargetNotFoundException("좌석 정보를 찾을 수 없습니다.");
        }

        if (!ScreeningStatus.ON_SALE.equals(isAvailableSeat.getFirst().screeningStatus())) {
            throw new ScreeningNotOnSaleException();
        }

        if (isAvailableSeat.stream().anyMatch(seat -> SeatStatus.AVAILABLE != seat.seatStatus())) {
            throw new SeatNotOnSaleException("판매 중인 좌석이 아닙니다.");
        }

        return isAvailableSeat;
    }

    /**
     * 예매에 연결된 좌석을 점유한다. flush 로 예매·좌석 INSERT 를 즉시 실행해
     * uk_seat_hold 유니크 충돌(이미 점유된 좌석)을 이 자리에서 감지한다.
     * 다른 요청이 같은 좌석을 처리 중이면 잠금 대기(innodb_lock_wait_timeout)가 초과되거나 교착이 날 수 있는데,
     * 이 역시 "이미 다른 사람이 잡은 좌석"으로 안내한다.
     */
    @Transactional
    public List<MovieReservationSeat> putIfNotTaken(List<Long> seatIdList, MovieReservation reservation, Screening screening) {
        // 여러 좌석을 항상 같은 순서(좌석 ID 오름차순)로 INSERT 해 요청 간 교착을 줄인다
        List<MovieReservationSeat> reservationSeatList = seatIdList.stream()
                .sorted()
                .map(theaterSeatRepository::getReferenceById)
                .map(seatRef -> MovieReservationSeat.create(reservation, screening, seatRef))
                .toList();

        try {
            reservationSeatRepository.saveAllAndFlush(reservationSeatList);
        }
        catch (DataIntegrityViolationException e) {
            throw new SeatAlreadyTakenException("이미 예매된 좌석입니다.");
        }
        // 잠금 대기 초과·교착, 또는 잠금을 기다리다 트랜잭션 시간(timeout)이 먼저 소진된 경우
        catch (PessimisticLockingFailureException | QueryTimeoutException e) {
            throw new SeatAlreadyTakenException("다른 요청이 처리 중인 좌석입니다.");
        }

        return reservationSeatList;
    }
}
