package com.example.cinema_middleware.v1.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * "refresh:{memberId}" 키 포맷과 TTL 처리 같은 Redis 구현 디테일은
 * 전부 이 클래스 안에만 존재합니다. 이 포맷을 바꾸고 싶으면 여기 한 곳만 고치면 됩니다.
 */
@Repository
@RequiredArgsConstructor
public class AuthTokenRepository {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";
    private static final String LOGOUT_VALUE = "logout";

    private final StringRedisTemplate redisTemplate;

    public void saveRefreshToken(Long memberId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(refreshTokenKey(memberId), refreshToken, ttl);
    }

    public Optional<String> findRefreshTokenByMemberId(Long memberId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(refreshTokenKey(memberId)));
    }

    public void deleteRefreshTokenByMemberId(Long memberId) {
        redisTemplate.delete(refreshTokenKey(memberId));
    }

    public void saveBlacklist(String accessToken, Duration ttl) {
        redisTemplate.opsForValue().set(blacklistKey(accessToken), LOGOUT_VALUE, ttl);
    }

    public Boolean includeBlacklistByAccessToken(String accessToken) {
        return redisTemplate.hasKey(blacklistKey(accessToken));
    }

    private String blacklistKey(String accessToken) {
        return BLACKLIST_KEY_PREFIX + accessToken;
    }

    private String refreshTokenKey(Long memberId) {
        return REFRESH_TOKEN_KEY_PREFIX + memberId;
    }
}
