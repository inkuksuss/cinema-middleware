package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.request.SignUpRequest;
import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.service.MemberService;
import com.example.cinema_middleware.v1.service.dto.AddMemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/member/")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/sign-up")
    public ResponseEntity<Result<Long>> signUp(@RequestBody @Validated SignUpRequest request) {
        AddMemberDto addMemberDto = new AddMemberDto();
        addMemberDto.setEmail(request.email());
        addMemberDto.setUsername(request.username());
        addMemberDto.setPassword(request.password());
        addMemberDto.setPhoneNumber(request.phoneNumber());
        addMemberDto.setBirthday(request.birthday());

        Long memberId = memberService.addMember(addMemberDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Result.ofSuccess(memberId));
    }

    @PostMapping("/test")
    public Long test() {
        return 1L;
    }
}
