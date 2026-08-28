package com.example.cinema_middleware.v1.security.jwt;

import com.example.cinema_middleware.v1.controller.response.ResponseCode;
import com.example.cinema_middleware.v1.controller.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 인증되지 않은 상태로 보호된 API를 호출했을 때 실행됩니다.
 * 프로젝트의 GlobalExceptionAdvice와 동일하게 JSON 바디로 응답합니다.
 * (Result/ResponseCode 클래스 시그니처를 확인하지 못해 여기서는 Map으로 응답을 구성했습니다.
 *  실제 프로젝트의 Result.of(code, message, data) 형태로 바꿔서 사용하세요.)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Result body = Result.of(ResponseCode.UNAUTHORIZED.getCode(), "로그인 후 시도해주세요.", null);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
