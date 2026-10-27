package com.example.cinema_middleware.v1.controller.request;

import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatStatus;
import com.example.cinema_middleware.v1.service.dto.SeatCommand;
import com.example.cinema_middleware.v1.service.dto.TheaterCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상영관 API 요청 DTO 모음.
 * Request -> Command 변환은 이 계층에서 한다(controller -> service 단방향 유지).
 */
public final class TheaterRequest {

    private TheaterRequest() {
    }

    public record Create(
            @NotNull(message = "cinemaId은(는) 필수 입력 항목입니다.")
            Long cinemaId,

            @NotBlank(message = "name은(는) 필수 입력 항목입니다.")
            String name,

            @NotBlank(message = "description은(는) 필수 입력 항목입니다.")
            String description
    ) {
        public TheaterCommand.Create toCommand() {
            return new TheaterCommand.Create(
                    this.cinemaId,
                    this.name,
                    this.description
            );
        }
    }

    public record Modify(
            @NotBlank(message = "name은(는) 필수 입력 항목입니다.")
            String name,

            @NotBlank(message = "description은(는) 필수 입력 항목입니다.")
            String description
    ) {
    }

    public record BulkCreateSeat(
            @NotEmpty(message = "seatList은(는) 필수 입력 항목입니다.")
            @Size(max = 400, message = "좌석 갯수는 최대 400개입니다.")
            @Valid
            List<CreateSeat> seatList
    ) {
        public record CreateSeat(
                @NotBlank(message = "seatRow은(는) 필수 입력 항목입니다.")
                String seatRow,

                @NotBlank(message = "seatColumn은(는) 필수 입력 항목입니다.")
                String seatColumn,

                @NotNull(message = "grade은(는) 필수 입력 항목입니다.")
                SeatGrade grade,

                BigDecimal price,

                @NotNull(message = "status은(는) 필수 입력 항목입니다.")
                SeatStatus status
        ) {
        }

        /** theaterId 는 PathVariable 로 들어오므로 인자로 받는다 */
        public TheaterCommand.BulkAddSeat toCommand(Long theaterId) {
            return new TheaterCommand.BulkAddSeat(
                    theaterId,
                    seatList.stream()
                            .map(seat -> new SeatCommand.Create(
                                    seat.seatRow(), seat.seatColumn(), seat.grade(), seat.price, seat.status))
                            .toList()
            );
        }
    }
}
