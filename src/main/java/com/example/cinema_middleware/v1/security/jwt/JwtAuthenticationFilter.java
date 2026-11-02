package com.example.cinema_middleware.v1.security.jwt;

import com.example.cinema_middleware.v1.controller.response.ResponseCode;
import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.repository.AuthTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import static com.example.cinema_middleware.v1.security.AuthorizationConst.*;

/**
 * 매 요청마다 Authorization 헤더의 Bearer 토큰을 검사해서
 * 유효하면 SecurityContext에 인증 정보를 채워 넣는 필터.
 * 세션을 쓰지 않으므로(STATELESS) 매 요청마다 이 과정이 반복됩니다.
 *
 * 로그아웃 블랙리스트(Redis)를 확인할 수 없으면 요청을 막는다(fail-closed).
 * 로그아웃한 토큰이 쓰이는 것을 막기 위함이며, 토큰이 없는 요청은 Redis 를 거치지 않아 영향이 없다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenRepository authTokenRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String accessToken = jwtTokenProvider.resolveToken(request.getHeader(HEADER));

        if (StringUtils.hasText(accessToken) && jwtTokenProvider.isValid(accessToken)) {
            Boolean blacklisted;
            try {
                blacklisted = authTokenRepository.includeBlacklistByAccessToken(accessToken);
            }
            catch (DataAccessException e) {
                // 필터는 MVC 밖이라 GlobalExceptionAdvice 가 적용되지 않으므로 여기서 직접 응답한다
                log.error("[jwt filter] 블랙리스트 조회 실패로 요청을 차단합니다.", e);
                writeServiceUnavailable(response);
                return;
            }

            if (Boolean.FALSE.equals(blacklisted)) {
                authenticate(accessToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String accessToken) {
        Long memberId = jwtTokenProvider.getMemberId(accessToken);
        String role = jwtTokenProvider.parseClaims(accessToken).get("role", String.class);

        List<SimpleGrantedAuthority> authorities = role != null
                ? List.of(new SimpleGrantedAuthority(role))
                : List.of();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void writeServiceUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Result<Void> body = Result.of(ResponseCode.SERVICE_UNAVAILABLE.getCode(), "잠시 후 다시 시도해주세요.", null);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
