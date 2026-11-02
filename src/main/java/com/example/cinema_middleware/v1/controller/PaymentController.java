package com.example.cinema_middleware.v1.controller;

import com.example.cinema_middleware.v1.controller.request.PaymentRequest;
import com.example.cinema_middleware.v1.controller.response.ResponseCode;
import com.example.cinema_middleware.v1.controller.response.Result;
import com.example.cinema_middleware.v1.service.PaymentService;
import com.example.cinema_middleware.v1.support.exception.IllegalReservationStateException;
import com.example.cinema_middleware.v1.support.exception.ReservationConflictException;
import com.example.cinema_middleware.v1.support.exception.ReservationExpiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<Result<UUID>> doPayment(@RequestBody @Validated PaymentRequest.DoPayment request) {
        try {
            UUID result = paymentService.process(request.toCommand());

            return ResponseEntity
                    .ok()
                    .body(Result.ofSuccess(result));
        }
        catch (IllegalReservationStateException e) {
            return ResponseEntity
                    .ok()
                    .body(Result.of(ResponseCode.ILLEGAL_STATE.getCode(), "확정할 수 없는 예매정보입니다.", null));
        }
        catch (ReservationExpiredException e) {
            return ResponseEntity
                    .ok()
                    .body(Result.of(ResponseCode.RESERVATION_EXPIRED.getCode(), "이미 만료 된 예매입니다.", null));
        }
        catch (ReservationConflictException e) {
            return ResponseEntity
                    .ok()
                    .body(Result.of(ResponseCode.ILLEGAL_STATE.getCode(), "이미 처리 중이거나 처리된 예매입니다.", null));
        }
    }


}
