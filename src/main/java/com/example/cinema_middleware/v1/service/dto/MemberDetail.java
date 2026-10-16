package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.Member;
import com.example.cinema_middleware.v1.domain.entity.enums.MemberGrade;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

@Getter
@ToString
public class MemberDetail {

    private Long id;

    private String email;

    private String username;

    private MemberGrade grade;

    public static MemberDetail from(Member member) {
        Assert.notNull(member, "member can not be null");

        MemberDetail memberDetail = new MemberDetail();
        memberDetail.id = member.getId();
        memberDetail.email = member.getEmail();
        memberDetail.username = member.getUsername();
        memberDetail.grade = member.getGrade();

        return memberDetail;
    }
}
