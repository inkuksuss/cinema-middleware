package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.repository.MovieReservationRepository;
import com.example.cinema_middleware.v1.repository.dao.ReservationDao;
import com.example.cinema_middleware.v1.repository.dao.search.ReservationCondition;
import com.example.cinema_middleware.v1.service.dto.ReservationCommand;
import com.example.cinema_middleware.v1.service.dto.ReservationDto;
import com.example.cinema_middleware.v1.service.factory.ReservationDtoFactory;
import com.example.cinema_middleware.v1.support.exception.TargetNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class AdminMovieReservationService {

    private final MovieReservationRepository reservationRepository;

    public boolean checkHavingAliveReservation(Long screeningId) {
        Assert.notNull(screeningId, "screeningId can not be null");

        return reservationRepository.existsAliveByScreeningId(screeningId);
    }

    public Page<ReservationDto.AdminSummary> getReservationSummary(ReservationCommand.SearchAdminRow search) {
        Assert.notNull(search, "search can not be null");

        ReservationCondition.AdminRow condition = new ReservationCondition.AdminRow(
                search.pageable(),
                search.startDate(), search.endDate(),
                search.status(), search.code()
        );

        return reservationRepository.findAdminRow(condition)
                .map(ReservationDtoFactory::adminSummary);
    }

    public ReservationDto.AdminDetail getReservationDetail(Long reservationId) {
        ReservationDao.AdminRowDetail detail = reservationRepository.findAdminDetail(reservationId)
                .orElseThrow(() -> {
                    throw new TargetNotFoundException();
                });

        return ReservationDtoFactory.adminDetail(detail);
    }

    // TODO: 예매 취소 구현 필요
    public void cancel(Long reservationId) {
        throw new UnsupportedOperationException("미구현");
    }
}
