package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.BaseEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SQLRestriction("is_delete = 'N'")
@SQLDelete(sql = "UPDATE theater_seat SET is_delete = 'Y' WHERE theater_seat_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TheaterSeat extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theater_seat_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id")
    private Theater theater;

    @Column(nullable = false, length = 100)
    private String seatRow;

    @Column(nullable = false, length = 100)
    private String seatColumn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private SeatGrade grade;
}
