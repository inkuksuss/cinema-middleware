package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.enums.ReservationSeatStatus;
import com.example.cinema_middleware.v1.repository.dao.SeatDao;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.repository.query.Param;

import java.util.List;

import static com.example.cinema_middleware.v1.domain.entity.QMovieReservationSeat.movieReservationSeat;
import static com.example.cinema_middleware.v1.domain.entity.QScreening.screening;
import static com.example.cinema_middleware.v1.domain.entity.QTheater.theater;
import static com.example.cinema_middleware.v1.domain.entity.QTheaterSeat.theaterSeat;

public class TheaterSeatRepositoryImpl implements TheaterSeatRepositoryQuery {


    private final JPAQueryFactory queryFactory;

    public TheaterSeatRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<SeatDao.SeatCounter> findSeatCounterListByIdList(@Param("screeningIdList") List<Long> screeningIdList) {
        return queryFactory
                .select(Projections.constructor(
                        SeatDao.SeatCounter.class,
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
                                        movieReservationSeat.status.eq(ReservationSeatStatus.RESERVED)
                                )
                ))
                .from(screening)
                .where(screening.id.in(screeningIdList))
                .fetch();
    }

    @Override
    public List<SeatDao.Info> findAllOfScreening(@Param("screeningId") Long screeningId) {
        return queryFactory
                .select(Projections.constructor(
                        SeatDao.Info.class,
                        screening.id,
                        screening.status,

                        theaterSeat.id,
                        theaterSeat.seatRow,
                        theaterSeat.seatColumn,
                        theaterSeat.grade,
                        theaterSeat.price,

                        movieReservationSeat.id,
                        movieReservationSeat.status
                ))
                .from(screening)
                .join(screening.theater, theater)
                .join(theaterSeat)
                .on(theater.id.eq(theaterSeat.theater.id))
                .leftJoin(movieReservationSeat)
                .on(theaterSeat.id.eq(
                                movieReservationSeat.theaterSeat.id),
                        (movieReservationSeat.status.eq(ReservationSeatStatus.RESERVED))
                )
                .where(screening.id.eq(screeningId))
                .fetch();
    }

    @Override
    public List<SeatDao.Info> findIsAvailableSeat(@Param("screeningId") Long screeningId, @Param("theaterSeatIdList") List<Long> theaterSeatIdList) {
        return queryFactory
                .select(Projections.constructor(
                        SeatDao.Info.class,
                        screening.id,
                        screening.status,

                        theaterSeat.id,
                        theaterSeat.seatRow,
                        theaterSeat.seatColumn,
                        theaterSeat.grade,
                        theaterSeat.price,

                        movieReservationSeat.id,
                        movieReservationSeat.status
                ))
                .from(screening)
                .join(screening.theater, theater)
                .join(theaterSeat)
                .on(theater.id.eq(theaterSeat.theater.id))
                .leftJoin(movieReservationSeat)
                .on(
                        theaterSeat.id.eq(movieReservationSeat.theaterSeat.id),
                        (movieReservationSeat.status.eq(ReservationSeatStatus.RESERVED))
                )
                .where(
                        screening.id.eq(screeningId),
                        theaterSeat.id.in(theaterSeatIdList)
                )
                .fetch();
    }
}
