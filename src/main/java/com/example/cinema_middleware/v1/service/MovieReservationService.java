package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.*;
import com.example.cinema_middleware.v1.service.dto.MovieReservationSummary;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.repository.MovieReservationRepository;
import com.example.cinema_middleware.v1.repository.MovieReservationSeatRepository;
import com.example.cinema_middleware.v1.repository.ScreeningRepository;
import com.example.cinema_middleware.v1.repository.TheaterSeatRepository;
import com.example.cinema_middleware.v1.repository.dto.SeatPrice;
import com.example.cinema_middleware.v1.service.dto.MovieReservationDetail;
import com.example.cinema_middleware.v1.support.exception.ScreeningNotOnSaleException;
import com.example.cinema_middleware.v1.support.exception.SeatAlreadyTakenException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class MovieReservationService {

    private final AuthService authService;
    private final MovieReservationRepository movieReservationRepository;
    private final ScreeningRepository screeningRepository;
    private final MovieReservationSeatRepository movieReservationSeatRepository;
    private final TheaterSeatRepository theaterSeatRepository;

    @Transactional
    public MovieReservationDetail addReservation(Long screeningId, List<Long> theaterSeatIdList) {
        Member memberRef = authService.getMemberReference();
        Screening screeningRef = screeningRepository.getReferenceById(screeningId);
        List<SeatPrice> seatPriceList = screeningRepository.findSeatPriceListFor(screeningId, theaterSeatIdList);

        if (seatPriceList.size() == 0) {
            throw new NoSuchElementException();
        }

        if (seatPriceList.size() != theaterSeatIdList.size()) {
            throw new IllegalStateException();
        }

        if (!ScreeningStatus.ON_SALE.equals(seatPriceList.getFirst().getScreeningStatus())) {
            throw new ScreeningNotOnSaleException();
        }

        List<MovieReservationSeat> reservationSeatList = theaterSeatIdList.stream()
                .map(theaterSeatRepository::getReferenceById)
                .map(seatRef -> MovieReservationSeat.of(null, screeningRef, seatRef))
                .toList();

        try {
            movieReservationSeatRepository.saveAllAndFlush(reservationSeatList);
        }
        catch (DataIntegrityViolationException e) {
            throw new SeatAlreadyTakenException();
        }

        MovieReservation movieReservation = MovieReservation.of(
                memberRef,
                screeningRef,
                seatPriceList.stream().map(SeatPrice::getPrice).toList()
        );

        movieReservationRepository.save(movieReservation);

        reservationSeatList.forEach(rs -> rs.setReservation(movieReservation));

        return MovieReservationDetail.from(movieReservation);
    }

    public MovieReservationDetail confirmReservation(Long reservationId) {
        Member member = authService.getMemberReference();

        MovieReservation movieReservation = movieReservationRepository.findById(reservationId)
                .orElseThrow(() -> { throw new NoSuchElementException(); });

        if (!member.getId().equals(movieReservation.getMember().getId())) {
            throw new AccessDeniedException("access denied");
        }

        movieReservation.confirm();

        return MovieReservationDetail.from(movieReservation);
    }

    public Page<MovieReservationSummary> getReservationPageAtMember(Pageable pageable, @Nullable ReservationStatus status) {
        Long memberId = authService.getMemberId();

        return movieReservationRepository.findReservationPageAtMember(pageable, memberId, status)
                .map(movieReservation -> {
                    MovieReservationSummary movieReservationSummary = MovieReservationSummary.from(movieReservation);
                    movieReservationSummary.addMovieSummary(movieReservation.getScreening().getMovie());
                    movieReservationSummary.addScreeningSummary(movieReservation.getScreening());

                    return movieReservationSummary;
                });
    }
}
