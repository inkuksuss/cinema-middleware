package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.enums.YesOrNo;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.*;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

import static com.example.cinema_middleware.v1.domain.entity.QMovie.*;
import static com.example.cinema_middleware.v1.support.QueryDslUtils.*;

public class MovieRepositoryImpl implements MovieRepositoryQuery {

    private final JPAQueryFactory queryFactory;

    public MovieRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<Movie> findMoviePageByDate(Pageable pageable, LocalDate targetDate) {
        List<Movie> content = queryFactory
                .select(movie)
                .from(movie)
                .where(
                        movie.releaseDate.loe(targetDate),
                        movie.closeDate.goe(targetDate)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(movie.releaseDate.desc())
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
    public Slice<Movie> findMovieSliceByDate(Pageable pageable, LocalDate targetDate) {
        List<Movie> content = queryFactory
                .select(movie)
                .from(movie)
                .where(
                        movie.releaseDate.loe(targetDate),
                        movie.closeDate.goe(targetDate)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .orderBy(movie.releaseDate.desc())
                .fetch();

        return createSlice(content, pageable);
    }

    @Override
    public Page<Movie> findMoviePage(Pageable pageable, String title, YesOrNo isActive) {
        List<Movie> content = queryFactory
                .select(movie)
                .from(movie)
                .where(
                        titleContains(title),
                        isActiveEq(isActive)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(getSort(pageable, movie))
                .fetch();

        Long totalCount = queryFactory
                .select(movie.count())
                .from(movie)
                .where(
                        titleContains(title),
                        isActiveEq(isActive)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, totalCount);
    }

    private BooleanExpression titleContains(String title) {
        return StringUtils.hasText(title) ? movie.title.contains(title) : null;
    }

    private BooleanExpression isActiveEq(YesOrNo isActive) { return isActive != null ? movie.isActive.eq(isActive) : null; }
}
