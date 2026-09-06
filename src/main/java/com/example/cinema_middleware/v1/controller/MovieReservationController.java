package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.request.CreateReservationRequest;
import com.example.cinema_middleware.v1.controller.response.ResponseCode;
import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.service.MovieReservationService;
import com.example.cinema_middleware.v1.service.dto.MovieReservationDetail;
import com.example.cinema_middleware.v1.service.dto.MovieReservationSummary;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import com.example.cinema_middleware.v1.support.exception.ReservationExpiredException;
import com.example.cinema_middleware.v1.support.exception.ScreeningNotOnSaleException;
import com.example.cinema_middleware.v1.support.exception.SeatAlreadyTakenException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController("/api/v1/reservation")
@RequiredArgsConstructor
public class MovieReservationController {

    private final MovieReservationService movieReservationService;

    @PostMapping("/")
    public ResponseEntity<Result<MovieReservationDetail>> createReservation(@RequestBody @Validated CreateReservationRequest request) {
        try {
            MovieReservationDetail result =
                    movieReservationService.addReservation(request.screeningId(), request.theaterSeatIdList());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.ofSuccess(result));
        }
        catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.of(ResponseCode.NOT_FOUND.getCode(), "잘못된 좌석 혹은 상영입니다.", null));
        }
        catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.of(ResponseCode.INVALID_ARGUMENT.getCode(), "좌석 정보가 옳바르지 않습니다.", null));
        }
        catch (ScreeningNotOnSaleException e) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.of(ResponseCode.SCREENING_NOT_ON_SALE.getCode(), "현재 예매가 불가합니다.", null));
        }
        catch (SeatAlreadyTakenException e) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.of(ResponseCode.SEAT_ALREADY_TAKEN.getCode(), "이미 예매 된 좌석입니다.", null));
        }
    }

    @PostMapping("/{reservationId}/confirm")
    public ResponseEntity<Result<MovieReservationDetail>> confirmReservation(@PathVariable Long reservationId) {
        try {
            MovieReservationDetail result = movieReservationService.confirmReservation(reservationId);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.ofSuccess(result));
        }
        catch (AccessDeniedException e) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.of(ResponseCode.UNAUTHORIZED.getCode(), "접근할 수 없습니다.", null));
        }
        catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.of(ResponseCode.NOT_FOUND.getCode(), "접근할 수 없습니다.", null));
        }
        catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.of(ResponseCode.ILLEGAL_STATE.getCode(), "확정할 수 없는 예매정보입니다.", null));
        }
        catch (ReservationExpiredException e) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.of(ResponseCode.RESERVATION_EXPIRED.getCode(), "이미 만료 된 예매입니다.", null));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<Result<Page<MovieReservationSummary>>> getMyReservation(
            @PageableDefault(sort = "created", direction = Sort.Direction.DESC, size = 7) Pageable pageable,
            @RequestParam(required = false) ReservationStatus status
    ) {
        Page<MovieReservationSummary> result =
                movieReservationService.getReservationPageAtMember(pageable, status);

        return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Result.ofSuccess(result));
    }
}
