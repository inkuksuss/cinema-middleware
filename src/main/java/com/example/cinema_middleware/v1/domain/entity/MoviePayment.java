package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.DefaultEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

//TODO
@Entity
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class MoviePayment extends DefaultEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_reservation_id")
    private MovieReservation reservation;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal paidAmount;

    @Column(nullable = false, length = 100)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private PaymentStatus status;
}
