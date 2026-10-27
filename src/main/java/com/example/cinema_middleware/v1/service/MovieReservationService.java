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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieReservationService {

    private final AuthService authService;
    private final SeatService seatService;
    private final ScreeningService screeningService;
    private final MovieReservationRepository movieReservationRepository;


    @Transactional
    public Long addReservation(Long screeningId, List<Long> seatIdList) {
        Member memberRef = authService.getMemberRefer();
        Screening screeningRef = screeningService.getReferById(screeningId);

        List<SeatDao.Info> seatList = seatService.checkIsAvailable(screeningId, seatIdList);

        List<MovieReservationSeat> savedSeat = seatService.putIfNotTaken(seatIdList, screeningRef);

        MovieReservation newReservation = MovieReservation.create(
                memberRef, screeningRef, issueCode(),
                getSeatNames(seatList), getOriginalAmount(seatList)
        );
        MovieReservation savedReservation = movieReservationRepository.save(newReservation);

        seatService.putReservation(
                savedSeat.stream().map(MovieReservationSeat::getId).toList(),
                newReservation.getId()
        );

        return savedReservation.getId();
    }

    // TODO
    public ReservationDto.AdminDetail confirmReservation(Long reservationId) {
        Member member = authService.getMemberRefer();

        MovieReservation movieReservation = movieReservationRepository.findById(reservationId)
                .orElseThrow(() -> { throw new TargetNotFoundException(); });

        if (!member.getId().equals(movieReservation.getMember().getId())) {
            throw new AccessDeniedException("");
        }

        movieReservation.confirm();

        return null;
    }

    public Slice<ReservationDto.Summary> getMyReservations(Pageable pageable, @Nullable ReservationStatus status) {
        return movieReservationRepository.findMyReservationSlice(pageable, authService.getMemberId(), status)
                .map(ReservationDtoFactory::summary);
    }

    public ReservationDto.Detail getReservationDetail(Long reservationId) {
        ReservationDao.MyReservationDetail findReservation = movieReservationRepository.findMyReservationDetail(reservationId)
                .orElseThrow(() -> { throw new TargetNotFoundException(); });

        if (!(authService.getMemberId()).equals(findReservation.memberId())) throw new AccessDeniedException("Access Denied");

        return ReservationDtoFactory.detail(findReservation);
    }

    private String getSeatNames(List<SeatDao.Info> seatList) {
        return seatList.stream().map(seat -> seat.seatRow() + seat.seatColumn()).collect(Collectors.joining(", "));
    }

    private BigDecimal getOriginalAmount(List<SeatDao.Info> seatList) {
        return seatList.stream().map(SeatDao.Info::price).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String issueCode() {
        return UUID.randomUUID().toString();
    }
}
