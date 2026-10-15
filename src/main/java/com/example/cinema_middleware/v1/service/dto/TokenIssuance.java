package com.example.cinema_middleware.v1.service.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class TokenIssuance {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private long expiredTime;

    public static TokenIssuance of(String accessToken, String refreshToken, String tokenType, long expiredTime) {
        TokenIssuance tokenIssuance = new TokenIssuance();
        tokenIssuance.accessToken = accessToken;
        tokenIssuance.refreshToken = refreshToken;
        tokenIssuance.tokenType = tokenType;
        tokenIssuance.expiredTime = expiredTime;

        return tokenIssuance;
    }
}
