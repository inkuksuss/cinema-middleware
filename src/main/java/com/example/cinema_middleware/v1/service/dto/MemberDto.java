package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Member;
import com.example.cinema_middleware.v1.domain.entity.enums.MemberGrade;
import org.springframework.util.Assert;

/**
 * 회원 도메인 서비스 응답 DTO 모음.
 * 상위 계층(controller)을 참조하지 않는다.
 */
public final class MemberDto {

    private MemberDto() {
    }

    /** 내 정보 조회 응답 */
    public record Detail(
            Long id,
            String email,
            String username,
            MemberGrade grade
    ) {
        public static Detail from(Member member) {
            Assert.notNull(member, "member can not be null");

            return new Detail(
                    member.getId(),
                    member.getEmail(),
                    member.getUsername(),
                    member.getGrade()
            );
        }
    }
}
