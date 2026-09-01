package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.service.ScreeningService;
import com.example.cinema_middleware.v1.service.dto.ScreeningSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/screening")
public class ScreeningController {

    private final ScreeningService screeningService;

    @GetMapping("/summary")
    public ResponseEntity<Result<Page<ScreeningSummaryDto>>> getSummary(
            @RequestParam(value = "movieId", required = false) Long movieId,
            @RequestParam("targetDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate targetDate
    ) {
        Page<ScreeningSummaryDto> screeningSummary = screeningService.getSummaryByDate(targetDate, movieId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Result.ofSuccess(screeningSummary));
    }
}
