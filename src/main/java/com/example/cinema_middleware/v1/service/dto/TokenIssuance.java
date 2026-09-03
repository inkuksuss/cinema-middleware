package com.example.cinema_middleware.v1.service.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@ToString
public class TokenIssuance {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private long expiredTime;
}
