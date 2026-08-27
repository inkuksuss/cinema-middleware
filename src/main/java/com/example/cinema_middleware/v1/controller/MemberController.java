package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members/")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;


}
