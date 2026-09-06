package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Theater;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@ToString
public class TheaterSummary {

    private Long id;

    private String name;

    public static TheaterSummary from(Theater theater) {
        TheaterSummary theaterSummary = new TheaterSummary();
        theaterSummary.id = theater.getId();
        theaterSummary.name = theater.getName();

        return theaterSummary;
    }
}
