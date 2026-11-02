package com.example.cinema_middleware.v1.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CouponService {

    //TODO
    public BigDecimal apply(Long couponId, BigDecimal originalAmount) {
        return originalAmount;
    }
}
