package com.example.cinema_middleware.v1.service.dto;

/**
 * 회원 도메인 서비스 입력 커맨드.
 * 상위 계층(controller)을 참조하지 않는다 — 변환은 Request 쪽에서 한다.
 */
public final class MemberCommand {

    private MemberCommand() {
    }

    /** 회원 가입 */
    public record SignUp(
            String email,
            String username,
            String password,
            String phoneNumber,
            String birthday
    ) {
    }
}
