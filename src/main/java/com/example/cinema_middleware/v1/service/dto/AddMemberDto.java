package com.example.cinema_middleware.v1.service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddMemberDto {

    private String email;

    private String username;

    private String password;

    private String phoneNumber;

    private String birthday;
}
