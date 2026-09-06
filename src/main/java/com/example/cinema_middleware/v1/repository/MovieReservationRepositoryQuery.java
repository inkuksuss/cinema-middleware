package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface MovieReservationRepositoryQuery {

    Page<MovieReservation> findReservationPageAtMember(Pageable pageable, Long memberId, @Nullable ReservationStatus status);

    Slice<MovieReservation> findReservationSliceAtMember(Pageable pageable, Long memberId, @Nullable ReservationStatus status);
}
