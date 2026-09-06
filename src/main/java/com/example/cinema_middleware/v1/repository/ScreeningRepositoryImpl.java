package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.QMovieReservationSeat;
import com.example.cinema_middleware.v1.domain.entity.QTheaterSeat;
import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.repository.dto.ScreeningSeat;
import com.example.cinema_middleware.v1.repository.dto.SeatCount;
import com.example.cinema_middleware.v1.repository.dto.SeatPrice;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

import static com.example.cinema_middleware.v1.domain.entity.QMovie.*;
import static com.example.cinema_middleware.v1.domain.entity.QMovieReservationSeat.*;
import static com.example.cinema_middleware.v1.domain.entity.QScreening.screening;
import static com.example.cinema_middleware.v1.domain.entity.QTheater.*;
import static com.example.cinema_middleware.v1.domain.entity.QTheaterSeat.*;

public class ScreeningRepositoryImpl implements ScreeningRepositoryQuery {

    private final JPAQueryFactory queryFactory;

    public ScreeningRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<Screening> findScreeningListAtTargetDate(LocalDate targetDate, @Nullable Long movieId) {
        return queryFactory
                .select(screening)
                .from(screening)
                .join(screening.movie, movie)
                .join(screening.theater, theater)
                .fetchJoin()
                .where(
                        screening.startDate.eq(targetDate),
                        movieIdEq(movieId)
                )
                .orderBy(screening.startAt.asc())
                .fetch();
    }

    @Override
    public List<SeatCount> findSeatCountListByIdList(@Param("screeningIdList") List<Long> screeningIdList) {
        return queryFactory
                .select(Projections.constructor(
                        SeatCount.class,
                        screening.id,
                        JPAExpressions
                                .select(theaterSeat.count())
                                .from(theaterSeat)
                                .where(theaterSeat.theater.id.eq(screening.theater.id)),
                        JPAExpressions
                                .select(movieReservationSeat.count())
                                .from(movieReservationSeat)
                                .where(
                                        movieReservationSeat.screening.id.eq(screening.id),
                                        movieReservationSeat.isActive.eq("Y")
                                )
                ))
                .from(screening)
                .where(screening.id.in(screeningIdList))
                .fetch();
    }

    @Override
    public List<ScreeningSeat> findAllSeatByScreeningId(@Param("screeningId") Long screeningId) {
        return queryFactory
                .select(Projections.constructor(
                        ScreeningSeat.class,
                        screening.id,
                        theaterSeat.id,
                        theaterSeat.seatRow,
                        theaterSeat.seatColumn,
                        theaterSeat.grade,
                        theaterSeat.price,
                        movieReservationSeat.id,
                        movieReservationSeat.isActive
                ))
                .from(screening)
                .join(screening.theater, theater)
                .join(theaterSeat)
                .on(theater.id.eq(theaterSeat.theater.id))
                .leftJoin(movieReservationSeat)
                .on(theaterSeat.id.eq(movieReservationSeat.theaterSeat.id), (movieReservationSeat.isActive.eq("Y")))
                .where(screening.id.eq(screeningId))
                .fetch();
    }

    @Override
    public List<SeatPrice> findSeatPriceListFor(@Param("screeningId") Long screeningId, @Param("theaterSeatIdList") List<Long> theaterSeatIdList) {
        return queryFactory
                .select(Projections.constructor(
                        SeatPrice.class,
                        screening.id,
                        screening.status,
                        theaterSeat.id,
                        theaterSeat.grade,
                        theaterSeat.price
                ))
                .from(screening)
                .join(screening.theater, theater)
                .join(theaterSeat)
                .on(theater.id.eq(theaterSeat.theater.id), theaterSeat.id.in(theaterSeatIdList))
                .where(screening.id.eq(screeningId))
                .fetch();
    }

    private BooleanExpression movieIdEq(Long movieId) {
        return movieId != null ? movie.id.eq(movieId) : null;
    }
}
