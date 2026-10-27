package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Screening;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationSeatStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.repository.dao.*;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.example.cinema_middleware.v1.domain.entity.QCinema.cinema;
import static com.example.cinema_middleware.v1.domain.entity.QMovie.*;
import static com.example.cinema_middleware.v1.domain.entity.QMovieReservationSeat.movieReservationSeat;
import static com.example.cinema_middleware.v1.domain.entity.QScreening.screening;
import static com.example.cinema_middleware.v1.domain.entity.QTheater.*;
import static com.example.cinema_middleware.v1.domain.entity.QTheaterSeat.theaterSeat;

public class ScreeningRepositoryImpl implements ScreeningRepositoryQuery {

    private final JPAQueryFactory queryFactory;

    public ScreeningRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<Screening> findScreeningAtTargetDate(LocalDate targetDate, Long cinemaId, @Nullable Long movieId) {
        return queryFactory
                .select(screening)
                .from(screening)
                .join(screening.movie, movie).fetchJoin()
                .join(screening.theater, theater).fetchJoin()
                .join(theater.cinema, cinema).fetchJoin()
                .where(
                        screening.startDate.eq(targetDate),
                        screening.status.eq(ScreeningStatus.ON_SALE),
                        cinema.id.eq(cinemaId),
                        movieIdEq(movieId)
                )
                .orderBy(screening.startAt.asc())
                .fetch();
    }

    @Override
    public List<ScreeningDao.Preview> findPreviewAtTargetDate(LocalDate targetDate, Long cinemaId, @Nullable Long movieId) {
        return queryFactory
                .select(Projections.constructor(
                        ScreeningDao.Preview.class,
                        screening.id,
                        screening.startDate,
                        screening.startAt,
                        screening.screenType,
                        screening.status,

                        movie.id,
                        movie.title,
                        movie.runningTime,
                        movie.ageRating,
                        movie.posterUrl,

                        theater.id,
                        theater.name,

                        cinema.id,
                        cinema.name,

                        theaterSeat.count(),
                        movieReservationSeat.count()
                ))
                .from(screening)
                .join(screening.theater, theater)
                .join(theater.cinema, cinema)
                .join(screening.movie, movie)
                .join(theaterSeat.theater, theater)
                .leftJoin(theaterSeat.movieReservationSeatList)
                .on(
                        theaterSeat.id.eq(movieReservationSeat.theaterSeat.id),
                        movieReservationSeat.status.eq(ReservationSeatStatus.RESERVED)
                )
                .where(
                        screening.startDate.eq(targetDate),
                        screening.status.eq(ScreeningStatus.ON_SALE),
                        cinema.id.eq(cinemaId),
                        movieIdEq(movieId)
                )
                .groupBy(screening.id)
                .fetch();
    }

    @Override
    public Page<ScreeningDao.AdminRow> findAdminRow(Pageable pageable, LocalDate targetDate, @Nullable Long theaterId, @Nullable Long movieId) {
        List<ScreeningDao.AdminRow> content =  queryFactory
                .select(Projections.constructor(
                        ScreeningDao.AdminRow.class,
                        screening.id,
                        screening.startDate,
                        screening.startAt,
                        screening.status,
                        screening.screenType,

                        movie.id,
                        movie.title,
                        movie.runningTime,
                        movie.ageRating,

                        theater.id,
                        theater.name,

                        cinema.id,
                        cinema.name
                        )
                )
                .from(screening)
                .where(
                        screening.startDate.eq(targetDate),
                        movieIdEq(movieId),
                        theaterIdEq(theaterId)
                )
                .join(screening.movie, movie)
                .join(screening.theater, theater)
                .join(theater.cinema, cinema)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(screening.createdAt.desc())
                .fetch();

        Long count = queryFactory
                .select(screening.count())
                .from(screening)
                .join(screening.movie, movie)
                .join(screening.theater, theater)
                .join(theater.cinema, cinema)
                .where(
                        screening.startDate.eq(targetDate),
                        movieIdEq(movieId),
                        theaterIdEq(theaterId)
                )
                .fetchOne();


        return new PageImpl<>(content, pageable, count);
    }

    @Override
    public Optional<ScreeningDao.AdminRowDetail> findAdminDetail(Long screeningId) {
        ScreeningDao.AdminRowDetail content = queryFactory
                .select(
                        Projections.constructor(
                                ScreeningDao.AdminRowDetail.class,
                                screening.id,
                                screening.startDate,
                                screening.startAt,
                                screening.status,
                                screening.screenType,
                                screening.createdAt,
                                screening.updatedAt,
                                screening.createdBy,
                                screening.updatedBy,

                                movie.id,
                                movie.title,
                                movie.runningTime,
                                movie.ageRating,
                                movie.category,

                                theater.id,
                                theater.name,

                                cinema.id,
                                cinema.name
                        )
                ).from(screening)
                .join(screening.movie, movie)
                .join(screening.theater, theater)
                .join(theater.cinema, cinema)
                .where(screening.id.eq(screeningId))
                .fetchOne();

        return Optional.of(content);
    }


    @Override
    public boolean existsAlreadyUsing(Long theaterId, LocalDate date, LocalTime time) {
        return queryFactory
                .selectOne()
                .from(screening)
                .where(
                        screening.theater.id.eq(theaterId)
                                .and(
                                        (screening.startDate.gt(date)
                                                .or(
                                                        (screening.startDate.eq(date).and(screening.startAt.goe(time)))
                                                )
                                        )
                                )
                )
                .fetchFirst() != null;
    }

    private BooleanExpression movieIdEq(Long movieId) {
        return movieId != null ? movie.id.eq(movieId) : null;
    }

    private BooleanExpression theaterIdEq(Long theaterId) {
        return theaterId != null ? theater.id.eq(theaterId) : null;
    }
}
