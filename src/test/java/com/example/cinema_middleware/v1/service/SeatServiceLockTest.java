package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.TheaterSeat;
import com.example.cinema_middleware.v1.repository.ReservationSeatRepository;
import com.example.cinema_middleware.v1.repository.TheaterSeatRepository;
import com.example.cinema_middleware.v1.support.exception.SeatAlreadyTakenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.QueryTimeoutException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * {@link SeatService#putIfNotTaken} 단위 테스트. 잠금 대기 초과·교착·시간 초과를
 * "이미 예매된 좌석"(SeatAlreadyTakenException)으로 변환하는지 확인한다.
 */
class SeatServiceLockTest {

    private TheaterSeatRepository theaterSeatRepository;
    private ReservationSeatRepository reservationSeatRepository;
    private SeatService seatService;

    @BeforeEach
    void setUp() {
        theaterSeatRepository = mock(TheaterSeatRepository.class);
        reservationSeatRepository = mock(ReservationSeatRepository.class);
        seatService = new SeatService(theaterSeatRepository, reservationSeatRepository);
        when(theaterSeatRepository.getReferenceById(anyLong())).thenReturn(mock(TheaterSeat.class));
    }

    @Test
    @DisplayName("잠금 대기 초과·교착(CannotAcquireLockException)은 SeatAlreadyTakenException")
    void lockTimeout_toSeatAlreadyTaken() {
        when(reservationSeatRepository.saveAllAndFlush(any()))
                .thenThrow(new CannotAcquireLockException("Lock wait timeout exceeded"));

        assertThatThrownBy(() -> seatService.putIfNotTaken(
                List.of(2L, 1L), mock(MovieReservation.class), mock(Screening.class)))
                .isInstanceOf(SeatAlreadyTakenException.class);
    }

    @Test
    @DisplayName("잠금을 기다리다 트랜잭션 시간이 소진된 경우(QueryTimeoutException)도 SeatAlreadyTakenException")
    void queryTimeout_toSeatAlreadyTaken() {
        when(reservationSeatRepository.saveAllAndFlush(any()))
                .thenThrow(new QueryTimeoutException("Statement cancelled due to timeout"));

        assertThatThrownBy(() -> seatService.putIfNotTaken(
                List.of(1L), mock(MovieReservation.class), mock(Screening.class)))
                .isInstanceOf(SeatAlreadyTakenException.class);
    }
}
