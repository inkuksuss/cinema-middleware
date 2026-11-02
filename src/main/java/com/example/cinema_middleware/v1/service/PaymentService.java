package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.MoviePayment;
import com.example.cinema_middleware.v1.domain.entity.MovieReservation;
import com.example.cinema_middleware.v1.repository.PaymentRepository;
import com.example.cinema_middleware.v1.service.dto.PaymentCommand;
import com.example.cinema_middleware.v1.support.exception.ReservationConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MovieReservationService reservationService;
    private final CouponService couponService;


    @Transactional(timeout = 5)
    public UUID process(PaymentCommand.CreatePayment command) {
        MovieReservation reservation = reservationService.getPayableReservation(command.reservationId());

        MoviePayment payment = MoviePayment.create(
                reservation, UUID.randomUUID().toString(),
                couponService.apply(null, reservation.getOriginalAmount()),
                command.method(), command.status()
        );

        // 커밋 시점이 아닌 여기서 버전 검사를 실행해, 동시 확정 충돌을 도메인 예외로 변환한다.
        try {
            paymentRepository.saveAndFlush(payment);
        } catch (OptimisticLockingFailureException e) {
            throw new ReservationConflictException();
        }

        return reservation.getId();
    }
}
