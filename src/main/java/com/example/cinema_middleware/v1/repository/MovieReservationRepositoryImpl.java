package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.*;

import java.util.List;

import static com.example.cinema_middleware.v1.domain.entity.QMovie.*;
import static com.example.cinema_middleware.v1.domain.entity.QMovieReservation.*;
import static com.example.cinema_middleware.v1.domain.entity.QScreening.*;
import static com.example.cinema_middleware.v1.support.QueryDslUtils.createSlice;

public class MovieReservationRepositoryImpl implements MovieReservationRepositoryQuery {

    private final JPAQueryFactory queryFactory;

    public MovieReservationRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<MovieReservation> findReservationPageAtMember(Pageable pageable, Long memberId, @Nullable ReservationStatus status) {
        List<MovieReservation> content = queryFactory
                .select(movieReservation)
                .from(movieReservation)
                .join(movieReservation.screening, screening)
                .join(screening.movie, movie)
                .fetchJoin()
                .where(
                        movieReservation.member.id.eq(memberId),
                        reservationStatusEq(status)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(movieReservation.createdAt.desc())
                .fetch();

        Long count = queryFactory
                .select(movieReservation.count())
                .from(movieReservation)
                .where(
                        movieReservation.member.id.eq(memberId),
                        reservationStatusEq(status)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, count);
    }

    @Override
    public Slice<MovieReservation> findReservationSliceAtMember(Pageable pageable, Long memberId, @Nullable ReservationStatus status) {
        List<MovieReservation> content = queryFactory
                .select(movieReservation)
                .from(movieReservation)
                .join(movieReservation.screening, screening)
                .join(screening.movie, movie)
                .fetchJoin()
                .where(
                        movieReservation.member.id.eq(memberId),
                        reservationStatusEq(status)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .orderBy(movieReservation.createdAt.desc())
                .fetch();

        return createSlice(content, pageable);
    }

    private BooleanExpression reservationStatusEq(ReservationStatus status) {
        return status != null ? movieReservation.status.eq(status) : null;
    }
}
