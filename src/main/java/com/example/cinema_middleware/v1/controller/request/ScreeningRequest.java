package com.example.cinema_middleware.v1.controller.request;

import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.service.dto.ScreeningCommand;
import jakarta.validation.constraints.NotNull;

/**
 * 상영 API 요청 DTO 모음.
 * Request -> Command 변환은 이 계층에서 한다(controller -> service 단방향 유지).
 */
public final class ScreeningRequest {

    private ScreeningRequest() {
    }

    public record Modify(
            @NotNull(message = "status은(는) 필수 입력 항목입니다.")
            ScreeningStatus status
    ) {
        /** screeningId 는 PathVariable 로 들어오므로 인자로 받는다 */
        public ScreeningCommand.Update toCommand(Long screeningId) {
            return new ScreeningCommand.Update(screeningId, status);
        }
    }
}
