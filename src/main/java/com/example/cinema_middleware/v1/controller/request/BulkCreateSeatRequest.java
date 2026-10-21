package com.example.cinema_middleware.v1.controller.request;

import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import com.example.cinema_middleware.v1.service.dto.forms.BulkAddSeatForm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkCreateSeatRequest(
        @NotEmpty(message = "seatList은(는) 필수 입력 항목입니다.")
        @Size(max = 400, message = "좌석 갯수는 최대 400개입니다.")
        @Valid
        List<CreateSeatRequest> seatList
) {

    public record CreateSeatRequest(
            @NotBlank(message = "seatRow은(는) 필수 입력 항목입니다.")
            String seatRow,

            @NotBlank(message = "seatColumn은(는) 필수 입력 항목입니다.")
            String seatColumn,

            @NotNull(message = "grade은(는) 필수 입력 항목입니다.")
            SeatGrade grade
    ) {
    }

    public BulkAddSeatForm toForm() {
        BulkAddSeatForm form = new BulkAddSeatForm();
        seatList.forEach(seat ->
                form.getSeatList().add(BulkAddSeatForm.of(seat.seatRow(), seat.seatColumn(), seat.grade())));

        return form;
    }
}
