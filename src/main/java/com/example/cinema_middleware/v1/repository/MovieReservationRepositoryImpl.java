package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.repository.dto.ReservationAdminRow;
import com.example.cinema_middleware.v1.repository.dto.search.AdminReservationSummarySearch;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.*;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.List;

import static com.example.cinema_middleware.v1.domain.entity.QMember.*;
import static com.example.cinema_middleware.v1.domain.entity.QMovie.*;
import static com.example.cinema_middleware.v1.domain.entity.QMovieReservation.*;
import static com.example.cinema_middleware.v1.domain.entity.QScreening.*;
import static com.example.cinema_middleware.v1.domain.entity.QTheater.*;
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
                .leftJoin(movieReservation.screening, screening).fetchJoin()
                .leftJoin(screening.movie, movie).fetchJoin()
                .leftJoin(screening.theater, theater).fetchJoin()
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
                .leftJoin(movieReservation.screening, screening).fetchJoin()
                .leftJoin(screening.movie, movie).fetchJoin()
                .leftJoin(screening.theater, theater).fetchJoin()
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

    @Override
    public Page<ReservationAdminRow> findReservationAdminRow(AdminReservationSummarySearch search) {
        List<ReservationAdminRow> content = queryFactory
                .select(Projections.constructor(
                        ReservationAdminRow.class,
                        movieReservation.id,
                        movieReservation.member.id,
                        movieReservation.member.username,
                        movieReservation.code,
                        movieReservation.status,
                        movieReservation.createdAt,
                        movieReservation.updatedAt
                ))
                .from(movieReservation)
                .leftJoin(movieReservation.member, member)
                .where(
                        movieReservation.createdAt.goe(search.getStartDate().atStartOfDay()),
                        movieReservation.createdAt.loe(search.getEndDate().atTime(LocalTime.MAX)),
                        codeEq(search.getCode()),
                        reservationStatusEq(search.getStatus())
                )
                .offset(search.getPageable().getOffset())
                .limit(search.getPageable().getPageSize())
                .orderBy(movieReservation.createdAt.desc())
                .fetch();

        Long count = queryFactory
                .select(movieReservation.count())
                .from(movieReservation)
                .where(
                        movieReservation.createdAt.goe(search.getStartDate().atStartOfDay()),
                        movieReservation.createdAt.loe(search.getEndDate().atTime(LocalTime.MAX)),
                        codeEq(search.getCode()),
                        reservationStatusEq(search.getStatus())
                )
                .fetchOne();

        return new PageImpl<>(content, search.getPageable(), count);
    }


    @Override
    public boolean existsAliveByScreeningId(Long screeningId) {
        return queryFactory
                .selectOne()
                .from(movieReservation)
                .join(movieReservation.screening, screening)
                .where(
                        movieReservation.screening.id.eq(screeningId),
                        movieReservation.status.in(ReservationStatus.PENDING, ReservationStatus.SUCCESS)
                )
                .fetchFirst() != null;
    }

    private BooleanExpression reservationStatusEq(ReservationStatus status) {
        return status != null ? movieReservation.status.eq(status) : null;
    }

    private BooleanExpression codeEq(String code) {
        return StringUtils.hasText(code) ? movieReservation.code.eq(code) : null;
    }
}
