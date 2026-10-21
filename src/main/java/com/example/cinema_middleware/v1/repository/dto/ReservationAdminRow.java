package com.example.cinema_middleware.v1.repository.dto;

import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReservationAdminRow {

    private Long reservationId;

    private Long memberId;

    private String memberName;

    private String code;

    private ReservationStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
