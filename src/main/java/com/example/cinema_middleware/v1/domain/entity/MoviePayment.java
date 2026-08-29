package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

//TODO
@Entity
@SQLRestriction("is_delete = 'N'")
@SQLDelete(sql = "UPDATE movie_payment SET is_delete = 'Y' WHERE movie_payment_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MoviePayment extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_reservation_id")
    private MovieReservation reservation;
}
