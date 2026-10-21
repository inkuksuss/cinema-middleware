package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.*;
import com.example.cinema_middleware.v1.service.dto.*;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.repository.MovieReservationRepository;
import com.example.cinema_middleware.v1.repository.MovieReservationSeatRepository;
import com.example.cinema_middleware.v1.repository.ScreeningRepository;
import com.example.cinema_middleware.v1.repository.TheaterSeatRepository;
import com.example.cinema_middleware.v1.repository.dto.SeatData;
import com.example.cinema_middleware.v1.support.exception.ScreeningNotOnSaleException;
import com.example.cinema_middleware.v1.support.exception.SeatAlreadyTakenException;
import com.example.cinema_middleware.v1.support.exception.TargetNotFoundException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

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

        List<SeatData> seatDataList = screeningRepository.findSeatDataListFor(screeningId, theaterSeatIdList);

        if (seatDataList.size() == 0 || seatDataList.size() != theaterSeatIdList.size()) {
            throw new TargetNotFoundException("좌석 정보를 찾을 수 없습니다.");
        }

        if (!ScreeningStatus.ON_SALE.equals(seatDataList.getFirst().getScreeningStatus())) {
            throw new ScreeningNotOnSaleException();
        }

        List<MovieReservationSeat> reservationSeatList = theaterSeatIdList.stream()
                .map(theaterSeatRepository::getReferenceById)
                .map(seatRef -> MovieReservationSeat.create(null, screeningRef, seatRef))
                .toList();

        try {
            movieReservationSeatRepository.saveAllAndFlush(reservationSeatList);
        }
        catch (DataIntegrityViolationException e) {
            throw new SeatAlreadyTakenException("이미 예매된 좌석입니다.");
        }

        MovieReservation createdReservation = MovieReservation.create(memberRef, screeningRef, seatDataList);
        movieReservationRepository.save(createdReservation);
        reservationSeatList.forEach(rs -> rs.setReservation(createdReservation));

        return MovieReservationDetail.from(createdReservation);
    }

    // TODO
    public AdminMovieReservationDetail confirmReservation(Long reservationId) {
        Member member = authService.getMemberReference();

        MovieReservation movieReservation = movieReservationRepository.findById(reservationId)
                .orElseThrow(() -> { throw new TargetNotFoundException(); });

        if (!member.getId().equals(movieReservation.getMember().getId())) {
            throw new AccessDeniedException("access denied");
        }

        movieReservation.confirm();

        return AdminMovieReservationDetail.from(movieReservation);
    }

    public Page<MovieReservationSummary> getReservationPageAtMember(Pageable pageable, @Nullable ReservationStatus status) {
        return movieReservationRepository.findReservationPageAtMember(pageable, authService.getMemberId(), status)
                .map(movieReservation -> {
                    MovieReservationSummary movieReservationSummary = MovieReservationSummary.from(movieReservation);
                    movieReservationSummary.setMovieSummary(MovieDto.Summary.from(movieReservation.getScreening().getMovie()));
                    movieReservationSummary.setScreeningSummary(ScreeningSummary.from(movieReservation.getScreening()));

                    return movieReservationSummary;
                });
    }

    public MovieReservationDetail getReservationDetail(Long reservationId) {
        MovieReservation findReservation = movieReservationRepository.findDetailById(reservationId)
                .orElseThrow(() -> { throw new TargetNotFoundException("존재하지 않는 티켓 정보입니다."); });

        if (!(authService.getMemberId()).equals(findReservation.getMember().getId())) throw new AccessDeniedException("잘못된 접근입니다.");

        MovieReservationDetail movieReservationDetail = MovieReservationDetail.from(findReservation);

        findReservation.getPaymentList().stream()
                .max(Comparator.comparing(MoviePayment::getCreatedAt))
                .ifPresent(payment -> movieReservationDetail.setPaymentSummary(PaymentSummary.from(payment)));

        return movieReservationDetail;
    }
}
