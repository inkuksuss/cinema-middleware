package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.DefaultEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import com.example.cinema_middleware.v1.domain.entity.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@SQLRestriction("status != 'DELETE'")
@SQLDelete(sql = "UPDATE theater_seat SET status = 'DELETE' WHERE theater_seat_id = ?")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class TheaterSeat extends DefaultEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theater_seat_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id")
    private Theater theater;

    @OneToMany(mappedBy = "theaterSeat")
    private List<MovieReservationSeat> movieReservationSeatList = new ArrayList<>();

    @Column(nullable = false, length = 100)
    private String seatRow;

    @Column(nullable = false, length = 100)
    private String seatColumn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SeatGrade grade;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SeatStatus status;

    public static TheaterSeat create(Theater theater, String seatRow, String seatColumn, SeatGrade grade) {
        Assert.notNull(theater, "theater can not be null");
        Assert.hasText(seatRow, "seatRow can not be null");
        Assert.hasText(seatColumn, "seatColumn can not be null");
        Assert.notNull(grade, "grade can not be null");

        TheaterSeat theaterSeat = new TheaterSeat();
        theaterSeat.theater = theater;
        theaterSeat.seatRow = seatRow;
        theaterSeat.seatColumn = seatColumn;
        theaterSeat.grade = grade;
        theaterSeat.status = SeatStatus.AVAILABLE;

        return theaterSeat;
    }
}
