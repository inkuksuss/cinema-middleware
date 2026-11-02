package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.PaymentMethod;
import com.example.cinema_middleware.v1.domain.entity.enums.PaymentStatus;

import java.util.UUID;

public final class PaymentCommand {

    private PaymentCommand() {
    }

    public record CreatePayment(
            UUID reservationId,
            PaymentMethod method,
            PaymentStatus status
    ) {
    }
}
