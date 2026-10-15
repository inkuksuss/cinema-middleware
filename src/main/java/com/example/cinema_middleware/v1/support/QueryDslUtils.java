package com.example.cinema_middleware.v1.support;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

public class QueryDslUtils {

    public static <T> Slice<T> createSlice(List<T> content, Pageable pageable) {
        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content = content.subList(0, pageable.getPageSize());
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    public static <T> OrderSpecifier<?>[] getSort(Pageable pageable, EntityPathBase<T> qClass) {
        PathBuilder<T> pathBuilder = new PathBuilder<>(qClass.getType(), qClass.getMetadata());

        return pageable.getSort().stream().map(order -> new OrderSpecifier<>(
                order.isAscending() ? Order.ASC : Order.DESC,
                pathBuilder.getString(order.getProperty())
        )).toArray(OrderSpecifier[]::new);
    }

}
