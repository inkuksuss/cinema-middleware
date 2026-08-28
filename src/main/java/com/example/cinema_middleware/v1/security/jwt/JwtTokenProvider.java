package com.example.cinema_middleware.v1.security.jwt;

import com.example.cinema_middleware.v1.security.AuthorizationConst;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private Key key;

    @PostConstruct
    void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long memberId, String email, String role) {
        return createToken(memberId, email, role, jwtProperties.accessTokenExpireSeconds());
    }

    public String createRefreshToken(Long memberId) {
        return createToken(memberId, null, null, jwtProperties.refreshTokenExpireSeconds());
    }

    private String createToken(Long memberId, String email, String role, long expireSeconds) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + expireSeconds * 1000);

        JwtBuilder builder = Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .setIssuedAt(now)
                .setExpiration(expiredAt);

        if (email != null) builder.claim("email", email);
        if (role != null) builder.claim("role", role);

        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    public Claims parseClaims(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(this.resolveToken(token));

        return jws.getBody();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);

            return true;
        } catch (Exception e) {
            // 만료(ExpiredJwtException), 서명불일치, 형식오류 등을 모두 "무효 토큰"으로 취급
            return false;
        }
    }

    public long getRemainingSeconds(String token) {
        try {
            Date expiration = parseClaims(this.resolveToken(token)).getExpiration();
            return Math.max(0, (expiration.getTime() - System.currentTimeMillis()) / 1000);
        } catch (ExpiredJwtException e) {
            return 0;
        }
    }

    public Long getMemberId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String resolveToken(String token) {
        return StringUtils.hasText(token) && token.startsWith(AuthorizationConst.PREFIX) ?
                token.substring(AuthorizationConst.PREFIX.length()) :
                token;
    }
}
