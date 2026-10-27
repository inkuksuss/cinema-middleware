package com.example.cinema_middleware.v1.service.factory;

import jakarta.annotation.Nullable;

import java.util.function.Function;

/**
 * DTO Factory 공통 유틸.
 */
final class FactorySupport {

    private FactorySupport() {
    }

    /** source 가 null 이면 null, 아니면 getter 결과를 반환한다(연관 엔티티가 null 일 수 있는 평탄화 매핑용). */
    static <S, R> R nullSafe(@Nullable S source, Function<S, R> getter) {
        return source == null ? null : getter.apply(source);
    }
}
