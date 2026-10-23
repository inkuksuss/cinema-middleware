package com.example.cinema_middleware.v1.service.dto;

import org.springframework.util.Assert;

/**
 * 인증 도메인 서비스 응답 DTO 모음.
 * 상위 계층(controller)을 참조하지 않는다.
 */
public final class AuthDto {

    private AuthDto() {
    }

    /** 로그인·재발급 시 발급되는 토큰 쌍 */
    public record TokenIssuance(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiredTime
    ) {
        public TokenIssuance {
            Assert.hasText(accessToken, "accessToken can not be null");
            Assert.hasText(refreshToken, "refreshToken can not be null");
            Assert.notNull(tokenType, "tokenType can not be null");
        }
    }
}
