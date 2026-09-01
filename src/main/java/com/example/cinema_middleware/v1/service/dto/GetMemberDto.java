package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.MemberGrade;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class GetMemberDto {

    private Long id;

    private String email;

    private String username;

    private MemberGrade grade;
}
