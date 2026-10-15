package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.BaseEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

//TODO
@Entity
@SQLRestriction("is_delete = 'N'")
@SQLDelete(sql = "UPDATE movie_payment SET is_delete = 'Y' WHERE movie_payment_id = ?")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class MoviePayment extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_payment_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_reservation_id")
    private MovieReservation reservation;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private PaymentStatus status;

    @Column(nullable = false, length = 100)
    private String method;
}
