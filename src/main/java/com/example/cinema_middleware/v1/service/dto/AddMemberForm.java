package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.controller.request.SignUpRequest;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class AddMemberForm {

    private String email;

    private String username;

    private String password;

    private String phoneNumber;

    private String birthday;

    public static AddMemberForm from(SignUpRequest request) {
        AddMemberForm form = new AddMemberForm();

        form.email = request.email();
        form.username = request.username();
        form.password = request.password();
        form.phoneNumber = request.phoneNumber();
        form.birthday = request.birthday();

        return form;
    }
}
