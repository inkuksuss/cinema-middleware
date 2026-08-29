package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Member;
import com.example.cinema_middleware.v1.repository.AuthTokenRepository;
import com.example.cinema_middleware.v1.repository.MemberRepository;
import com.example.cinema_middleware.v1.security.MemberPrincipal;
import com.example.cinema_middleware.v1.security.AuthorizationConst;
import com.example.cinema_middleware.v1.security.jwt.JwtTokenProvider;
import com.example.cinema_middleware.v1.service.dto.TokenIssueDto;
import com.example.cinema_middleware.v1.support.exception.InvalidAccessTokenException;
import com.example.cinema_middleware.v1.support.exception.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static org.springframework.security.core.context.SecurityContextHolder.*;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final AuthTokenRepository authTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;


    public TokenIssueDto login(String email, String password) {
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        MemberPrincipal principal = (MemberPrincipal) authenticate.getPrincipal();

        return this.issueTokens(principal.getId(), principal.getUsername(), principal.getRole());
    }

    public TokenIssueDto reissue(String refreshToken) {
        if (!jwtTokenProvider.isValid(refreshToken)) {
            throw new InvalidRefreshTokenException("유효하지 않거나 만료된 refreshToken 입니다.");
        }

        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        String savedToken = authTokenRepository.findRefreshTokenByMemberId(memberId)
                .orElseThrow(() -> new InvalidRefreshTokenException("만료되었거나 이미 사용된 refreshToken 입니다."));

        // Redis에 저장된 refreshToken과 요청으로 들어온 값이 정확히 같아야 통과
        // (로그아웃했거나, 이미 한 번 재발급에 사용된 토큰이면 저장값과 달라짐 - 재사용 방지)
        if (savedToken == null || !savedToken.equals(refreshToken)) {
            throw new InvalidRefreshTokenException("만료되었거나 이미 사용된 refreshToken 입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new InvalidRefreshTokenException("잘못된 토큰입니다."));

        return this.issueTokens(member.getId(), member.getEmail(), member.getGrade().name());
    }


    public void logout(String accessToken) {
        Long memberId = (Long) getContext().getAuthentication().getPrincipal();
        if (memberId == null) {
            throw new InvalidAccessTokenException("잘못된 인증 토큰입니다.");
        }

        authTokenRepository.deleteRefreshTokenByMemberId(memberId);

        // access token은 만료 전까지 클라이언트가 계속 재사용할 수 있으므로
        // 남은 유효시간만큼 블랙리스트에 등록해서 즉시 무효화합니다.
        long remaining = jwtTokenProvider.getRemainingSeconds(accessToken);
        if (remaining > 0) {
            authTokenRepository.saveBlacklist(accessToken, Duration.ofSeconds(remaining));
        }
    }


    private TokenIssueDto issueTokens(Long memberId, String email, String grade) {
        String accessToken = jwtTokenProvider.createAccessToken(memberId, email, grade);
        String refreshToken = jwtTokenProvider.createRefreshToken(memberId);

        authTokenRepository.saveRefreshToken(
                memberId,
                refreshToken,
                Duration.ofSeconds(jwtTokenProvider.getJwtProperties().refreshTokenExpireSeconds())
        );

        return new TokenIssueDto(
                accessToken,
                refreshToken,
                AuthorizationConst.PREFIX,
                jwtTokenProvider.getJwtProperties().accessTokenExpireSeconds()
        );
    }
}
