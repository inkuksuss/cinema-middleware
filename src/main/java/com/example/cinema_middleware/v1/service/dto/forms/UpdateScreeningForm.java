package com.example.cinema_middleware.v1.service.dto.forms;

import com.example.cinema_middleware.v1.domain.entity.enums.ScreeningStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@ToString
public class UpdateScreeningForm {

    private Long screeningId;

    private ScreeningStatus status;
}
