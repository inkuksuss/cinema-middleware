package com.example.cinema_middleware.v1.config;

import com.example.cinema_middleware.v1.repository.AuthTokenRepository;
import com.example.cinema_middleware.v1.security.jwt.JwtAuthenticationEntryPoint;
import com.example.cinema_middleware.v1.security.jwt.JwtAuthenticationFilter;
import com.example.cinema_middleware.v1.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final AuthTokenRepository authTokenRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CustomUserDetailsService + PasswordEncoder 빈이 하나씩만 있으면
     * Spring Security가 자동으로 DaoAuthenticationProvider를 구성해서
     * 이 AuthenticationManager에 물려줍니다. 별도 Provider 빈 등록이 필요 없습니다.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/reissue", "/api/v1/sign-up").permitAll()
                                .requestMatchers("/api/v1/member/test").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, authTokenRepository),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
