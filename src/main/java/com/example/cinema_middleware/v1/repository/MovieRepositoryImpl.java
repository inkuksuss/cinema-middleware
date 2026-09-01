package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.List;

import static com.example.cinema_middleware.v1.domain.entity.QMovie.*;

public class MovieRepositoryImpl implements MovieRepositoryQuery {

    private final JPAQueryFactory queryFactory;

    public MovieRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<Movie> findSummaryWithPage(Pageable pageable, LocalDate targetDate) {
        List<Movie> content = queryFactory
                .select(movie)
                .from(movie)
                .where(
                        movie.releaseDate.loe(targetDate),
                        movie.closeDate.goe(targetDate)
                )
                .orderBy(movie.releaseDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(movie.count())
                .from(movie)
                .where(
                        movie.releaseDate.loe(targetDate),
                        movie.closeDate.goe(targetDate)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public Slice<Movie> findSummaryWithSlice(Pageable pageable, LocalDate targetDate) {
        List<Movie> content = queryFactory
                .select(movie)
                .from(movie)
                .where(
                        movie.releaseDate.loe(targetDate),
                        movie.closeDate.goe(targetDate)
                )
                .orderBy(movie.releaseDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content = content.subList(0, pageable.getPageSize());
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }
}
