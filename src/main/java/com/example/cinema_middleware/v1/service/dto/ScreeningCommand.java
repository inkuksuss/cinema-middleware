package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;

/**
 * 상영 도메인 서비스 입력 커맨드.
 * 상위 계층(controller)을 참조하지 않는다 — 변환은 Request 쪽에서 한다.
 */
public final class ScreeningCommand {

    private ScreeningCommand() {
    }

    /** 상영 상태 변경 — screeningId 는 PathVariable 로 들어온다 */
    public record Update(
            Long screeningId,
            ScreeningStatus status
    ) {
    }
}
