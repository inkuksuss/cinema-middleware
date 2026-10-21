package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.repository.dto.search.AdminReservationSummarySearch;
import com.example.cinema_middleware.v1.service.AdminMovieReservationService;
import com.example.cinema_middleware.v1.service.dto.AdminMovieReservationDetail;
import com.example.cinema_middleware.v1.service.dto.AdminMovieReservationSummary;
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
    public ResponseEntity<Result<Page<AdminMovieReservationSummary>>> getReservationSummary(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 7) Pageable pageable,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) String code
    ) {
        AdminReservationSummarySearch search = new AdminReservationSummarySearch(pageable, startDate, endDate);
        search.setStatus(status);
        search.setCode(code);

        Page<AdminMovieReservationSummary> result = reservationService.getReservationSummary(search);

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess(result));
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<Result<AdminMovieReservationDetail>> getReservationDetail(@PathVariable Long reservationId) {
        AdminMovieReservationDetail result = reservationService.getReservationDetail(reservationId);

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
