package com.example.cinema_middleware.v1.service.dto;

import lombok.Getter;

@Getter
public class TokenIssueDto {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private long expiredTime;

    public TokenIssueDto(String accessToken, String refreshToken, String tokenType, long expiredTime) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiredTime = expiredTime;
    }
}
