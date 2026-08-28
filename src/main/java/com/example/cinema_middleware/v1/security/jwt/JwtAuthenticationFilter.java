package com.example.cinema_middleware.v1.security.jwt;

import com.example.cinema_middleware.v1.repository.AuthTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static com.example.cinema_middleware.v1.security.AuthorizationConst.*;

/**
 * 매 요청마다 Authorization 헤더의 Bearer 토큰을 검사해서
 * 유효하면 SecurityContext에 인증 정보를 채워 넣는 필터.
 * 세션을 쓰지 않으므로(STATELESS) 매 요청마다 이 과정이 반복됩니다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenRepository authTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String accessToken = jwtTokenProvider.resolveToken(request.getHeader(HEADER));

        if (StringUtils.hasText(accessToken)
                && jwtTokenProvider.isValid(accessToken)
                && Boolean.FALSE.equals(authTokenRepository.includeBlacklistByAccessToken(accessToken))) {

            Long memberId = jwtTokenProvider.getMemberId(accessToken);
            String role = jwtTokenProvider.parseClaims(accessToken).get("role", String.class);

            List<SimpleGrantedAuthority> authorities = role != null
                    ? List.of(new SimpleGrantedAuthority(role))
                    : List.of();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(memberId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
