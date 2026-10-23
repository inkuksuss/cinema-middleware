package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.MemberGrade;

/**
 * 회원 도메인 서비스 응답 DTO 모음.
 * 상위 계층(controller)을 참조하지 않는다. 엔티티 변환은 {@code service.factory.MemberDtoFactory} 가 담당한다.
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
    }
}
