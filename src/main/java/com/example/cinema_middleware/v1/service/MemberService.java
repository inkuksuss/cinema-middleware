package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Member;
import com.example.cinema_middleware.v1.repository.MemberRepository;
import com.example.cinema_middleware.v1.service.dto.AddMemberForm;
import com.example.cinema_middleware.v1.service.dto.MemberDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public void addMember(AddMemberForm addMemberForm) {
        Member member = Member.create(
                addMemberForm.getEmail(),
                addMemberForm.getUsername(),
                passwordEncoder.encode(addMemberForm.getPassword()),
                addMemberForm.getPhoneNumber(),
                addMemberForm.getBirthday()
        );

        memberRepository.save(member);
    }

    public MemberDetail getMember(String accessToken) {
        Member findMember = authService.getMemberByAccessToken(accessToken);

        return MemberDetail.from(findMember);
    }
}
