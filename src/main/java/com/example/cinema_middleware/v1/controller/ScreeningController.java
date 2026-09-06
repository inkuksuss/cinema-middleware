package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.service.ScreeningService;
import com.example.cinema_middleware.v1.service.dto.ScreeningSummary;
import com.example.cinema_middleware.v1.service.dto.SeatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/screening")
public class ScreeningController {

    private final ScreeningService screeningService;

    @GetMapping("/summary")
    public ResponseEntity<Result<List<ScreeningSummary>>> getSummary(
            @RequestParam(value = "movieId", required = false) Long movieId,
            @RequestParam("targetDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate targetDate
    ) {
        List<ScreeningSummary> result = screeningService.getScreeningSummaryByDate(targetDate, movieId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Result.ofSuccess(result));
    }

    @GetMapping("/{screeningId}/seat")
    public ResponseEntity<Result<SeatMapper>> getScreeningSeat(@PathVariable Long screeningId) {
        SeatMapper seatMapper = screeningService.getScreeningSeat(screeningId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Result.ofSuccess(seatMapper));
    }
}
