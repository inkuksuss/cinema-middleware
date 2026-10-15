package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Member;
import com.example.cinema_middleware.v1.domain.entity.enums.MemberGrade;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MemberDetail {

    private Long id;

    private String email;

    private String username;

    private MemberGrade grade;

    public static MemberDetail from(Member member) {
        MemberDetail memberDetail = new MemberDetail();
        memberDetail.id = member.getId();
        memberDetail.email = member.getEmail();
        memberDetail.username = member.getUsername();
        memberDetail.grade = member.getGrade();

        return memberDetail;
    }
}
