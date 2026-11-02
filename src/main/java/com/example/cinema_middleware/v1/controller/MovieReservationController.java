package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.request.ReservationRequest;
import com.example.cinema_middleware.v1.controller.response.ResponseCode;
import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.service.MovieReservationService;
import com.example.cinema_middleware.v1.service.dto.ReservationDto;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.support.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservation")
@RequiredArgsConstructor
public class MovieReservationController {

    private final MovieReservationService movieReservationService;

    @PostMapping("/")
    public ResponseEntity<Result<UUID>> createReservation(@RequestBody @Validated ReservationRequest.Create request) {
        try {
            UUID result =
                    movieReservationService.addReservation(request.screeningId(), request.theaterSeatIdList());

            return ResponseEntity
                    .ok()
                    .body(Result.ofSuccess(result));
        }
        catch (ScreeningNotOnSaleException e) {
            return ResponseEntity
                    .ok()
                    .body(Result.of(ResponseCode.SCREENING_NOT_ON_SALE.getCode(), "현재 예매가 불가합니다.", null));
        }
        catch (SeatNotOnSaleException e) {
            return ResponseEntity
                    .ok()
                    .body(Result.of(ResponseCode.SEAT_NOT_ON_SALE.getCode(), "판매 중인 좌석이 아닙니다.", null));
        }
        catch (SeatAlreadyTakenException e) {
            return ResponseEntity
                    .ok()
                    .body(Result.of(ResponseCode.SEAT_ALREADY_TAKEN.getCode(), "이미 예매 된 좌석입니다.", null));
        }
        catch (DuplicateSeatException e) {
            return ResponseEntity
                    .ok()
                    .body(Result.of(ResponseCode.DUPLICATE_SEAT.getCode(), "중복된 좌석이 포함되어 있습니다.", null));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<Result<Slice<ReservationDto.Summary>>> getMyReservation(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 7) Pageable pageable,
            @RequestParam(required = false) ReservationStatus status
    ) {
        Slice<ReservationDto.Summary> result =
                movieReservationService.getMyReservations(pageable, status);

        return ResponseEntity
                    .ok()
                    .body(Result.ofSuccess(result));
    }

    @GetMapping("/{reservationId}/detail")
    public ResponseEntity<Result<ReservationDto.Detail>> getReservationDetail(
            @PathVariable UUID reservationId
    ) {
        ReservationDto.Detail result = movieReservationService.getReservationDetail(reservationId);

        return ResponseEntity
                .ok()
                .body(Result.ofSuccess(result));
    }
}
