package com.example.cinema_middleware.v1.controller.request;

import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import com.example.cinema_middleware.v1.service.dto.forms.UpdateScreeningForm;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ModifyScreeningRequest(
        @NotNull(message = "status은(는) 필수 입력 항목입니다.")
        ScreeningStatus status
) {
        public UpdateScreeningForm toForm() {
                UpdateScreeningForm form = new UpdateScreeningForm();
                form.setStatus(status);
                return form;
        }
}
