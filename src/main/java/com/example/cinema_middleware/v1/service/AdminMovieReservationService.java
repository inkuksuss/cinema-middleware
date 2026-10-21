package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.repository.MovieReservationRepository;
import com.example.cinema_middleware.v1.repository.dto.ReservationAdminRow;
import com.example.cinema_middleware.v1.repository.dto.search.AdminReservationSummarySearch;
import com.example.cinema_middleware.v1.service.dto.ReservationCommand;
import com.example.cinema_middleware.v1.service.dto.ReservationDto;
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

    // TODO: ReservationAdminRow -> ReservationDto.AdminSummary 매핑 구현 필요
    //  (조회 쿼리는 reservationRepository.findReservationAdminRow(search) 사용)
    public Page<ReservationDto.AdminSummary> getReservationSummary(ReservationCommand.AdminSearch command) {
        Assert.notNull(command, "adminSearchCommand can not be null");

        throw new UnsupportedOperationException("미구현");
    }

    /** 커맨드를 리포지토리 전용 검색 조건으로 변환한다(컨트롤러가 repository 를 모르게 한다). */
    private AdminReservationSummarySearch toSearch(ReservationCommand.AdminSearch command) {
        AdminReservationSummarySearch search = new AdminReservationSummarySearch(
                command.pageable(), command.startDate(), command.endDate());
        search.setStatus(command.status());
        search.setCode(command.code());

        return search;
    }

    // TODO: 예매 상세 조회 구현 필요
    public ReservationDto.AdminDetail getReservationDetail(Long reservationId) {
        throw new UnsupportedOperationException("미구현");
    }

    // TODO: 예매 취소 구현 필요
    public void cancel(Long reservationId) {
        throw new UnsupportedOperationException("미구현");
    }
}
