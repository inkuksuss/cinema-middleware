package com.example.cinema_middleware.v1.repository.dto.search;

import com.example.cinema_middleware.v1.domain.entity.enums.ReservationStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

@Getter @Setter
@ToString
public class AdminReservationSummarySearch {

    private Pageable pageable;

    private LocalDate startDate;

    private LocalDate endDate;

    private ReservationStatus status;

    private String code;

    public AdminReservationSummarySearch(Pageable pageable, LocalDate startDate, LocalDate endDate) {
        this.pageable = pageable;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
