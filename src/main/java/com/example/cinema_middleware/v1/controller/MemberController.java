package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.request.SignUpRequest;
import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.service.MemberService;
import com.example.cinema_middleware.v1.service.dto.AddMemberForm;
import com.example.cinema_middleware.v1.service.dto.MemberDetail;
import com.example.cinema_middleware.v1.support.exception.InvalidAccessTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/member/")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/sign-up")
    public ResponseEntity<Result<Long>> signUp(@RequestBody @Validated SignUpRequest request) {
        AddMemberForm addMemberForm = new AddMemberForm();
        addMemberForm.setEmail(request.email());
        addMemberForm.setUsername(request.username());
        addMemberForm.setPassword(request.password());
        addMemberForm.setPhoneNumber(request.phoneNumber());
        addMemberForm.setBirthday(request.birthday());

        memberService.addMember(addMemberForm);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Result.ofSuccess());
    }

    @PostMapping("/me")
    public ResponseEntity<Result<MemberDetail>> getMe(@RequestHeader("Authorization") String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new InvalidAccessTokenException();
        }

        MemberDetail result = memberService.getMember(accessToken);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Result.ofSuccess(result));
    }
}
