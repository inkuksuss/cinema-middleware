package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.request.CreateTheaterRequest;
import com.example.cinema_middleware.v1.controller.request.ModifyTheaterRequest;
import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.service.AdminTheaterService;
import com.example.cinema_middleware.v1.service.dto.SeatMapper;
import com.example.cinema_middleware.v1.service.dto.TheaterSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/theater")
@RequiredArgsConstructor
public class AdminTheaterController {

    private final AdminTheaterService theaterService;

    @GetMapping("/")
    public ResponseEntity<Result<Page<TheaterSummary>>> getTheaterSummary(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 7) Pageable pageable
    ) {
        Page<TheaterSummary> result = theaterService.getTheaterSummary(pageable);

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess(result));
    }

    @PostMapping("/")
    public ResponseEntity<Result<Void>> createTheater(@RequestBody @Validated CreateTheaterRequest request
    ) {
        theaterService.addTheater(request.name(), request.description());

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess());
    }

    @PutMapping("/{theaterId}")
    public ResponseEntity<Result<Void>> modifyTheater(
            @PathVariable Long theaterId,
            @RequestBody @Validated ModifyTheaterRequest request
    ) {
        theaterService.update(theaterId, request.name(), request.description());

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess());
    }

    @DeleteMapping("/{theaterId}")
    public ResponseEntity<Result<Void>> deleteTheater(@PathVariable Long theaterId) {
        theaterService.remove(theaterId);

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess());
    }

    @GetMapping("/{theaterId}/seat")
    public ResponseEntity<Result<SeatMapper>> getScreeningSeat(@PathVariable Long theaterId) {

    }



}
