package com.example.cinema_middleware.v1.service.dto.forms;

import com.example.cinema_middleware.v1.domain.entity.enums.SeatGrade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@ToString
public class BulkAddSeatForm {

    private Long theaterId;

    private List<AddSeatForm> seatList = new ArrayList<>();

    public static AddSeatForm of(String row, String column, SeatGrade grade) {
        return new AddSeatForm(row, column, grade);
    }

    @Getter
    @ToString
    @AllArgsConstructor
    public static class AddSeatForm {
        private String seatRow;

        private String seatColumn;

        private SeatGrade grade;
    }
}
