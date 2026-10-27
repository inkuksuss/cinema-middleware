package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.enums.ReservationSeatStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import static com.example.cinema_middleware.v1.domain.entity.QMovieReservationSeat.*;

public class ReservationSeatRepositoryImpl implements ReservationSeatRepositoryQuery {

    private final JPAQueryFactory queryFactory;

    public ReservationSeatRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public boolean existsReservedSeat(Long theaterSeatId) {
        return queryFactory
                .selectOne()
                .from(movieReservationSeat)
                .where(movieReservationSeat.status.eq(ReservationSeatStatus.RESERVED))
                .fetchFirst() != null;
    }
}
