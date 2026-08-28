package com.example.cinema_middleware.v1.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 jwt.* 값을 바인딩합니다.
 * 메인 애플리케이션 클래스에 @ConfigurationPropertiesScan 어노테이션을 추가해야
 * 이 record가 빈으로 등록됩니다.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpireSeconds,
        long refreshTokenExpireSeconds
) {
}
