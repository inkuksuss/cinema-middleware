package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Theater;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

@Getter
@ToString
public class TheaterSummary {

    private Long id;

    private String name;

    public static TheaterSummary from(Theater theater) {
        Assert.notNull(theater, "theater can not be null");

        return of(theater.getId(), theater.getName());
    }

    /** 조회 결과(row)처럼 엔티티 없이 식별 정보만 있을 때 사용 */
    public static TheaterSummary of(Long id, String name) {
        TheaterSummary theaterSummary = new TheaterSummary();
        theaterSummary.id = id;
        theaterSummary.name = name;

        return theaterSummary;
    }
}
