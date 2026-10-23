package com.example.cinema_middleware.v1.domain.entity.enums;

import java.math.BigDecimal;

public enum SeatGrade {

    NORMAL(0), ADVANCED(5000);

    private final Integer price;

    SeatGrade(Integer price) {
        this.price = price;
    }

    public Integer getPrice() {
        return price;
    }
}
