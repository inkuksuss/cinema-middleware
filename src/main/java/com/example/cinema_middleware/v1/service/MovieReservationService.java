package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.*;
import com.example.cinema_middleware.v1.repository.dao.ReservationDao;
import com.example.cinema_middleware.v1.repository.dao.SeatDao;
import com.example.cinema_middleware.v1.service.dto.*;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.repository.MovieReservationRepository;
import com.example.cinema_middleware.v1.service.factory.ReservationDtoFactory;
import com.example.cinema_middleware.v1.support.exception.TargetNotFoundException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieReservationService {

    private final AuthService authService;
    private final SeatService seatService;
    private final ScreeningService screeningService;
    private final MovieReservationRepository movieReservationRepository;


    // 좌석 경쟁이 몰리는 경로라 5초 안에 끝나지 않으면 실패시켜 커넥션을 오래 붙잡지 않는다
    @Transactional(timeout = 5)
    public UUID addReservation(Long screeningId, List<Long> seatIdList) {
        Assert.notNull(screeningId, "screeningId can not be null");
        Assert.notEmpty(seatIdList, "seatIdList can not be empty");

        Member memberRef = authService.getMemberRefer();
        Screening screeningRef = screeningService.getReferById(screeningId);

        List<SeatDao.Info> seatList = seatService.checkIsAvailable(screeningId, seatIdList);

        // UUID v7 PK 는 persist 시점에 생성되므로 INSERT 전에 ID 가 확정된다.
        // 좌석을 처음부터 예매에 연결해 저장하고, flush 때 예매 → 좌석 순서로 INSERT 된다.
        MovieReservation savedReservation = movieReservationRepository.save(MovieReservation.create(
                memberRef, screeningRef,
                getSeatNames(seatList), getOriginalAmount(seatList)
        ));

        seatService.putIfNotTaken(seatIdList, savedReservation, screeningRef);

        return savedReservation.getId();
    }

    public Slice<ReservationDto.Summary> getMyReservations(Pageable pageable, @Nullable ReservationStatus status) {
        return movieReservationRepository.findMyReservationSlice(pageable, authService.getMemberId(), status)
                .map(ReservationDtoFactory::summary);
    }

    public ReservationDto.Detail getReservationDetail(UUID reservationId) {
        ReservationDao.MyReservationDetail findReservation = movieReservationRepository.findMyReservationDetail(reservationId)
                .orElseThrow(() -> { throw new TargetNotFoundException(); });

        if (!(authService.getMemberId()).equals(findReservation.memberId())) throw new TargetNotFoundException();

        return ReservationDtoFactory.detail(findReservation);
    }

    public MovieReservation getPayableReservation(UUID reservationId) {
        MovieReservation reservation = this.getReservation(reservationId);
        reservation.checkPayable();

        return reservation;
    }

    public MovieReservation getReservation(UUID reservationId) {
        MovieReservation findReservation = movieReservationRepository.findById(reservationId)
                .orElseThrow(() -> { throw new TargetNotFoundException(); });

        if (!(authService.getMemberId()).equals(findReservation.getMember().getId())) throw new TargetNotFoundException();

        return findReservation;
    }

    private String getSeatNames(List<SeatDao.Info> seatList) {
        return seatList.stream().map(seat -> seat.seatRow() + seat.seatColumn()).collect(Collectors.joining(", "));
    }

    private BigDecimal getOriginalAmount(List<SeatDao.Info> seatList) {
        return seatList.stream().map(SeatDao.Info::price).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
