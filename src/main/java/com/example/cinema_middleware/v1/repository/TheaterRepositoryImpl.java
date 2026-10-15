package com.example.cinema_middleware.v1.repository;

import com.example.cinema_middleware.v1.domain.entity.QTheater;
import com.example.cinema_middleware.v1.domain.entity.Theater;
import com.example.cinema_middleware.v1.support.QueryDslUtils;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.cinema_middleware.v1.domain.entity.QTheater.*;

public class TheaterRepositoryImpl implements TheaterRepositoryQuery {

    private final JPAQueryFactory queryFactory;

    public TheaterRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<Theater> findTheaterPage(Pageable pageable) {
        List<Theater> content = queryFactory
                .select(theater)
                .from(theater)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(QueryDslUtils.getSort(pageable, theater))
                .fetch();

        Long count = queryFactory
                .select(theater.count())
                .from(theater)
                .fetchOne();

        return new PageImpl<>(content, pageable, count);
    }
}
