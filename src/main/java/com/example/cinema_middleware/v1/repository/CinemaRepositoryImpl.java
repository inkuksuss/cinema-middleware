package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.support.exception.TargetNotFoundException;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import static com.example.cinema_middleware.v1.domain.entity.QCinema.cinema;
import static com.example.cinema_middleware.v1.domain.entity.QTheater.theater;

public class CinemaRepositoryImpl implements CinemaRepositoryQuery {

    private final JPAQueryFactory queryFactory;

    public CinemaRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public boolean existsDuplicateTheaterName(Long cinemaId, String name) {
        Tuple tuple = queryFactory
                .select(cinema.id, theater.id)
                .from(cinema)
                .leftJoin(theater.cinema, cinema)
                .where(
                        cinema.id.eq(cinemaId),
                        theater.name.eq(name)
                )
                .fetchFirst();

        if (tuple == null || tuple.get(cinema.id) == null) throw new TargetNotFoundException("존재하지 않는 cinemaId입니다.");

        return tuple.get(theater.id) != null;
    }
}
