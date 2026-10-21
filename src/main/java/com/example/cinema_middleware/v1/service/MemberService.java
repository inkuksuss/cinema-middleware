package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Member;
import com.example.cinema_middleware.v1.domain.entity.enums.MemberGrade;
import com.example.cinema_middleware.v1.domain.entity.enums.SocialProvider;
import com.example.cinema_middleware.v1.repository.MemberRepository;
import com.example.cinema_middleware.v1.service.dto.MemberDto;
import com.example.cinema_middleware.v1.service.dto.MemberCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public void addMember(MemberCommand.SignUp command) {
        Member member = Member.create(
                command.email(),
                command.username(),
                passwordEncoder.encode(command.password()),
                command.phoneNumber(),
                command.birthday(),
                MemberGrade.ROLE_COMMON,
                SocialProvider.NORMAL
        );

        memberRepository.save(member);
    }

    public MemberDto.Detail getMember(String accessToken) {
        Member findMember = authService.getMemberByAccessToken(accessToken);

        return MemberDto.Detail.from(findMember);
    }
}
