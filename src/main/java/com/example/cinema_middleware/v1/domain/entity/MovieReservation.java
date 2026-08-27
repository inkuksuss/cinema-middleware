package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.BaseEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@SQLRestriction("is_delete = 'N'")
@SQLDelete(sql = "UPDATE movie_reservation SET is_delete = 'Y' WHERE movie_reservation_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MovieReservation extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_reservation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_id")
    private Screening screening;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private Integer seatCount;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private ReservationStatus status;

    private LocalDateTime expiredAt;

    private LocalDateTime canceledAt;
}
