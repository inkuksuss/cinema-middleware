package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.repository.MovieReservationRepository;
import com.example.cinema_middleware.v1.repository.dto.ReservationAdminRow;
import com.example.cinema_middleware.v1.repository.dto.search.AdminReservationSummarySearch;
import com.example.cinema_middleware.v1.service.dto.AdminMovieReservationDetail;
import com.example.cinema_middleware.v1.service.dto.AdminMovieReservationSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class AdminMovieReservationService {

    private final MovieReservationRepository reservationRepository;

    public boolean checkAliveReservation(Long screeningId) {
        Assert.notNull(screeningId, "screeningId can not be null");

        return reservationRepository.existsAliveByScreeningId(screeningId);
    }

    // TODO: ReservationAdminRow -> AdminMovieReservationSummary 매핑 구현 필요
    //  (조회 쿼리는 reservationRepository.findReservationAdminRow(search) 사용)
    public Page<AdminMovieReservationSummary> getReservationSummary(AdminReservationSummarySearch search) {
        throw new UnsupportedOperationException("미구현");
    }

    // TODO: 예매 상세 조회 구현 필요
    public AdminMovieReservationDetail getReservationDetail(Long reservationId) {
        throw new UnsupportedOperationException("미구현");
    }

    // TODO: 예매 취소 구현 필요
    public void cancel(Long reservationId) {
        throw new UnsupportedOperationException("미구현");
    }
}
