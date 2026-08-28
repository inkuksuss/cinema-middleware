package com.example.cinema_middleware.v1.security;

import com.example.cinema_middleware.v1.domain.entity.Member;
import com.example.cinema_middleware.v1.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * AuthenticationManager가 login() 시점의 이메일/비밀번호 검증에 사용합니다.
 * (비밀번호 대조는 PasswordEncoder를 통해 Spring Security가 알아서 처리합니다.)
 */
@Component
@RequiredArgsConstructor
public class UserDetailsHandler implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("이메일 또는 비밀번호가 일치하지 않습니다."));

        return new MemberPrincipal(member);
    }
}
