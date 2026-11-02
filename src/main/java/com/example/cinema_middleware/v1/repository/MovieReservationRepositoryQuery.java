package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.repository.dao.ReservationDao;
import com.example.cinema_middleware.v1.repository.dao.search.ReservationCondition;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;
import java.util.UUID;

public interface MovieReservationRepositoryQuery {

    Page<ReservationDao.MyReservation> findMyReservationPage(Pageable pageable, Long memberId, @Nullable ReservationStatus status);

    Slice<ReservationDao.MyReservation> findMyReservationSlice(Pageable pageable, Long memberId, @Nullable ReservationStatus status);

    Optional<ReservationDao.MyReservationDetail> findMyReservationDetail(UUID reservationId);

    Page<ReservationDao.AdminRow> findAdminRow(ReservationCondition.AdminRow adminRow);

    Optional<ReservationDao.AdminRowDetail> findAdminDetail(UUID reservationId);

    boolean existsAliveByScreeningId(Long screeningId);
}
