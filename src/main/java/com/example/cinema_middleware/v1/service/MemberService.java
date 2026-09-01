package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Member;
import com.example.cinema_middleware.v1.repository.MemberRepository;
import com.example.cinema_middleware.v1.service.dto.AddMemberDto;
import com.example.cinema_middleware.v1.service.dto.GetMemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public Long addMember(AddMemberDto addMemberDto) {
        Member member = new Member(
                addMemberDto.getEmail(),
                addMemberDto.getUsername(),
                passwordEncoder.encode(addMemberDto.getPassword()),
                addMemberDto.getPhoneNumber(),
                addMemberDto.getBirthday()
        );

        Member savedMember = memberRepository.save(member);

        return savedMember.getId();
    }

    public GetMemberDto getMember(String accessToken) {
        Member findMember = authService.getMemberByAccessToken(accessToken);

        GetMemberDto getMemberDto = new GetMemberDto();
        getMemberDto.setId(findMember.getId());
        getMemberDto.setEmail(findMember.getEmail());
        getMemberDto.setUsername(findMember.getUsername());
        getMemberDto.setGrade(findMember.getGrade());

        return getMemberDto;
    }
}
