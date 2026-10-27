package com.example.cinema_middleware.v1.service.factory;

import com.example.cinema_middleware.v1.domain.entity.Member;
import com.example.cinema_middleware.v1.service.dto.MemberDto;
import org.springframework.util.Assert;

/**
 * 회원 응답 DTO 조립기.
 * 엔티티 → {@link MemberDto} 변환의 유일한 지점이다.
 */
public final class MemberDtoFactory {

    private MemberDtoFactory() {
    }

    public static MemberDto.Detail detail(Member member) {
        Assert.notNull(member, "member can not be null");

        return new MemberDto.Detail(
                member.getId(),
                member.getEmail(),
                member.getUsername(),
                member.getGrade()
        );
    }
}
