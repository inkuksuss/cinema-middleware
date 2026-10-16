package com.example.cinema_middleware.v1.service.dto;

import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

@Getter
@ToString
public class TokenIssuance {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private long expiredTime;

    public static TokenIssuance of(String accessToken, String refreshToken, String tokenType, long expiredTime) {
        Assert.hasText(accessToken, "accessToken can not be null");
        Assert.hasText(refreshToken, "refreshToken can not be null");
        Assert.notNull(tokenType, "tokenType can not be null");

        TokenIssuance tokenIssuance = new TokenIssuance();
        tokenIssuance.accessToken = accessToken;
        tokenIssuance.refreshToken = refreshToken;
        tokenIssuance.tokenType = tokenType;
        tokenIssuance.expiredTime = expiredTime;

        return tokenIssuance;
    }
}
