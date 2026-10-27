package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.repository.dao.ReservationDao;
import com.example.cinema_middleware.v1.repository.dao.search.ReservationCondition;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.*;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.example.cinema_middleware.v1.domain.entity.QCinema.cinema;
import static com.example.cinema_middleware.v1.domain.entity.QCouponIssuance.*;
import static com.example.cinema_middleware.v1.domain.entity.QMember.*;
import static com.example.cinema_middleware.v1.domain.entity.QMovie.*;
import static com.example.cinema_middleware.v1.domain.entity.QMoviePayment.moviePayment;
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
    public Page<ReservationDao.MyReservation> findMyReservationPage(Pageable pageable, Long memberId, @Nullable ReservationStatus status) {
        List<ReservationDao.MyReservation> content = queryFactory
                .select(
                        Projections.constructor(
                                ReservationDao.MyReservation.class,
                                movieReservation.id,
                                movieReservation.code,
                                movieReservation.status,

                                movie.id,
                                movie.title,
                                movie.posterUrl,

                                theater.id,
                                theater.name,

                                screening.id,
                                screening.startDate,
                                screening.startAt,
                                screening.status,
                                screening.screenType,

                                cinema.id,
                                cinema.name
                        )
                )
                .from(movieReservation)
                .leftJoin(movieReservation.screening, screening)
                .leftJoin(screening.movie, movie)
                .leftJoin(screening.theater, theater)
                .leftJoin(theater.cinema, cinema)
                .leftJoin(movieReservation.member, member)
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
    public Slice<ReservationDao.MyReservation> findMyReservationSlice(Pageable pageable, Long memberId, @Nullable ReservationStatus status) {
        List<ReservationDao.MyReservation> content = queryFactory
                .select(
                        Projections.constructor(
                                ReservationDao.MyReservation.class,
                                movieReservation.id,
                                movieReservation.code,
                                movieReservation.status,

                                movie.id,
                                movie.title,
                                movie.posterUrl,

                                theater.id,
                                theater.name,

                                screening.id,
                                screening.startDate,
                                screening.startAt,
                                screening.status,
                                screening.screenType,

                                cinema.id,
                                cinema.name
                        )
                )
                .from(movieReservation)
                .leftJoin(movieReservation.screening, screening)
                .leftJoin(screening.movie, movie)
                .leftJoin(screening.theater, theater)
                .leftJoin(theater.cinema, cinema)
                .leftJoin(movieReservation.member, member)
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
    public Optional<ReservationDao.MyReservationDetail> findMyReservationDetail(Long reservationId) {
        ReservationDao.MyReservationDetail content = queryFactory
                .select(
                        Projections.constructor(
                                ReservationDao.MyReservationDetail.class,
                                movieReservation.id,
                                movieReservation.code,
                                movieReservation.originalAmount,
                                movieReservation.seatNames,
                                movieReservation.status,

                                member.id,

                                movie.id,
                                movie.title,

                                theater.id,
                                theater.name,

                                screening.id,
                                screening.startDate,
                                screening.startAt,
                                screening.status,
                                screening.screenType,

                                cinema.id,
                                cinema.name,

                                couponIssuance.id,

                                moviePayment.paidAmount,
                                moviePayment.method,
                                moviePayment.paidAt,
                                moviePayment.canceledAt,
                                moviePayment.status
                        )
                )
                .from(movieReservation)
                .leftJoin(movieReservation.screening, screening)
                .leftJoin(screening.movie, movie)
                .leftJoin(screening.theater, theater)
                .leftJoin(theater.cinema, cinema)
                .leftJoin(movieReservation.payment, moviePayment)
                .where(
                        movieReservation.id.eq(reservationId)
                )
                .fetchOne();

        return Optional.of(content);
    }

    @Override
    public Page<ReservationDao.AdminRow> findAdminRow(ReservationCondition.AdminRow condition) {
        List<ReservationDao.AdminRow> content = queryFactory
                .select(Projections.constructor(
                        ReservationDao.AdminRow.class,
                        movieReservation.id,
                        movieReservation.code,
                        movieReservation.status,
                        movieReservation.createdAt,
                        movieReservation.updatedAt,

                        member.id,
                        member.username,

                        movie.id,
                        movie.title,

                        theater.id,
                        theater.name,

                        screening.id,
                        screening.startDate,
                        screening.startAt,
                        screening.status,
                        screening.screenType,

                        cinema.id,
                        cinema.name
                ))
                .from(movieReservation)
                .leftJoin(movieReservation.member, member)
                .leftJoin(movieReservation.screening, screening)
                .leftJoin(screening.movie, movie)
                .leftJoin(screening.theater, theater)
                .leftJoin(theater.cinema, cinema)
                .where(
                        movieReservation.createdAt.goe(condition.startDate().atStartOfDay()),
                        movieReservation.createdAt.loe(condition.endDate().atTime(LocalTime.MAX)),
                        codeEq(condition.code()),
                        reservationStatusEq(condition.status())
                )
                .offset(condition.pageable().getOffset())
                .limit(condition.pageable().getPageSize())
                .orderBy(movieReservation.createdAt.desc())
                .fetch();

        Long count = queryFactory
                .select(movieReservation.count())
                .from(movieReservation)
                .where(
                        movieReservation.createdAt.goe(condition.startDate().atStartOfDay()),
                        movieReservation.createdAt.loe(condition.endDate().atTime(LocalTime.MAX)),
                        codeEq(condition.code()),
                        reservationStatusEq(condition.status())
                )
                .fetchOne();

        return new PageImpl<>(content, condition.pageable(), count);
    }

    @Override
    public Optional<ReservationDao.AdminRowDetail> findAdminDetail(Long reservationId) {
        ReservationDao.AdminRowDetail content = queryFactory
                .select(Projections.constructor(
                        ReservationDao.AdminRowDetail.class,
                        movieReservation.id,
                        movieReservation.code,
                        movieReservation.status,
                        movieReservation.createdAt,
                        movieReservation.updatedAt,

                        member.id,
                        member.username,

                        movie.id,
                        movie.title,

                        theater.id,
                        theater.name,

                        screening.id,
                        screening.startDate,
                        screening.startAt,
                        screening.status,
                        screening.screenType,

                        cinema.id,
                        cinema.name,

                        couponIssuance.id,

                        moviePayment.paidAmount,
                        moviePayment.method,
                        moviePayment.paidAt,
                        moviePayment.canceledAt,
                        moviePayment.status
                ))
                .from(movieReservation)
                .leftJoin(movieReservation.member, member)
                .leftJoin(movieReservation.screening, screening)
                .leftJoin(screening.movie, movie)
                .leftJoin(screening.theater, theater)
                .leftJoin(theater.cinema, cinema)
                .leftJoin(movieReservation.payment, moviePayment)
                .where(movieReservation.id.eq(reservationId))
                .fetchOne();

        return Optional.of(content);
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
