package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.service.ScreeningService;
import com.example.cinema_middleware.v1.service.dto.ScreeningDto;
import com.example.cinema_middleware.v1.service.dto.SeatDto;
import com.example.cinema_middleware.v1.service.dto.TheaterDto;
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

    @GetMapping("/")
    public ResponseEntity<Result<List<ScreeningDto.Summary>>> getScreeningSummary(
            @RequestParam("targetDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate targetDate,
            @RequestParam(value = "cinemaId") Long cinemaId,
            @RequestParam(value = "movieId", required = false) Long movieId
    ) {
        List<ScreeningDto.Summary> result = screeningService.getSummaryByDate(targetDate, cinemaId, movieId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Result.ofSuccess(result));
    }

    @GetMapping("/{screeningId}/seat")
    public ResponseEntity<Result<TheaterDto.ScreenMap>> getScreenMapForReservation(@PathVariable Long screeningId) {
        TheaterDto.ScreenMap result = screeningService.getScreenMapForReservation(screeningId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Result.ofSuccess(result));
    }
}
