package com.example.cinema_middleware.v1.service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class IssueTokenDto {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private long expiredTime;
}
