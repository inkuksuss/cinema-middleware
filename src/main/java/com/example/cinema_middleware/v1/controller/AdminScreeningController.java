package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.request.ModifyScreeningRequest;
import com.example.cinema_middleware.v1.controller.response.ResponseCode;
import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.service.AdminScreeningService;
import com.example.cinema_middleware.v1.service.dto.ScreeningDto;
import com.example.cinema_middleware.v1.service.dto.forms.UpdateScreeningForm;
import com.example.cinema_middleware.v1.support.exception.ScreeningHaveReservationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/screening")
@RequiredArgsConstructor
public class AdminScreeningController {

    private final AdminScreeningService screeningService;

    @GetMapping("/")
    public ResponseEntity<Result<Page<ScreeningDto.AdminSummary>>> getScreeningSummary(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 7) Pageable pageable,
            @RequestParam LocalDate targetDate,
            @RequestParam(required = false) Long theaterId,
            @RequestParam(required = false) Long movieId

    ) {
        Page<ScreeningDto.AdminSummary> result = screeningService.getSummary(pageable, targetDate, theaterId, movieId);

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess(result));
    }

    @GetMapping("/{screeningId}")
    public ResponseEntity<Result<ScreeningDto.AdminDetail>> getScreeningDetail(@PathVariable Long screeningId) {
        ScreeningDto.AdminDetail result = screeningService.getDetail(screeningId);

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess(result));
    }

    @PatchMapping("/{screeningId}")
    public ResponseEntity<Result<Void>> modifyScreening(
            @PathVariable Long screeningId,
            @RequestBody @Validated ModifyScreeningRequest request
    ) {
        UpdateScreeningForm form = request.toForm();
        form.setScreeningId(screeningId);

        try {
            screeningService.update(form);

            return ResponseEntity
                    .ok()
                    .body(Result.ofSuccess());
        }
        catch (ScreeningHaveReservationException e) {
            return ResponseEntity
                    .ok()
                    .body(Result.of(ResponseCode.SCREENING_CANT_UPDATE.getCode(), "이미 예매 된 상영입니다.", null));
        }
    }

    @DeleteMapping("/{screeningId}")
    public ResponseEntity<Result<Void>> deleteScreening(@PathVariable Long screeningId) {
        try {
            screeningService.remove(screeningId);

            return ResponseEntity
                    .ok()
                    .body(Result.ofSuccess());
        }
        catch (ScreeningHaveReservationException e) {
            return ResponseEntity
                    .ok()
                    .body(Result.of(ResponseCode.SCREENING_CANT_UPDATE.getCode(), "이미 예매 된 상영입니다.", null));
        }
    }
}
