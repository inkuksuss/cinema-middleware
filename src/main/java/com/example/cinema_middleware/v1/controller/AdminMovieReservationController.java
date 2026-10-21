package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.service.AdminMovieReservationService;
import com.example.cinema_middleware.v1.service.dto.ReservationCommand;
import com.example.cinema_middleware.v1.service.dto.ReservationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/reservation")
@RequiredArgsConstructor
public class AdminMovieReservationController {

    private final AdminMovieReservationService reservationService;

    @GetMapping("/")
    public ResponseEntity<Result<Page<ReservationDto.AdminSummary>>> getReservationSummary(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 7) Pageable pageable,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) String code
    ) {
        Page<ReservationDto.AdminSummary> result = reservationService.getReservationSummary(
                new ReservationCommand.AdminSearch(pageable, startDate, endDate, status, code));

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess(result));
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<Result<ReservationDto.AdminDetail>> getReservationDetail(@PathVariable Long reservationId) {
        ReservationDto.AdminDetail result = reservationService.getReservationDetail(reservationId);

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess(result));
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<Result<Void>> cancel(@PathVariable Long reservationId) {
        reservationService.cancel(reservationId);

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess());
    }
}
