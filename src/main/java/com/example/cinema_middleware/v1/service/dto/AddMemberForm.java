package com.example.cinema_middleware.v1.service.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@ToString
public class AddMemberForm {

    private String email;

    private String username;

    private String password;

    private String phoneNumber;

    private String birthday;
}
