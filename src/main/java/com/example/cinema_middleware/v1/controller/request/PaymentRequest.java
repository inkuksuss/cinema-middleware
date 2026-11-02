package com.example.cinema_middleware.v1.controller.request;

import com.example.cinema_middleware.v1.domain.entity.enums.PaymentMethod;
import com.example.cinema_middleware.v1.domain.entity.enums.PaymentStatus;
import com.example.cinema_middleware.v1.service.dto.PaymentCommand;
import com.example.cinema_middleware.v1.service.dto.ReservationCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public final class PaymentRequest {

    public record DoPayment(
            @NotNull(message = "reservationId 필수 입력 항목입니다.")
            UUID reservationId,
            @NotNull(message = "method 필수 입력 항목입니다.")
            PaymentMethod method,
            @NotNull(message = "status 필수 입력 항목입니다.")
            PaymentStatus status
    ) {
        public PaymentCommand.CreatePayment toCommand() {
            return new PaymentCommand.CreatePayment(reservationId, method, status);
        }
    }
}
