package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Theater;
import com.example.cinema_middleware.v1.domain.entity.TheaterSeat;
import com.example.cinema_middleware.v1.repository.MovieReservationSeatRepository;
import com.example.cinema_middleware.v1.repository.TheaterSeatRepository;
import com.example.cinema_middleware.v1.service.dto.TheaterDto;
import com.example.cinema_middleware.v1.service.dto.TheaterCommand;
import com.example.cinema_middleware.v1.support.exception.SeatInUseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSeatService {

    private final TheaterSeatRepository seatRepository;
    private final MovieReservationSeatRepository reservationSeatRepository;

    public List<TheaterDto.SeatSummary> getAllByTheaterId(Long theaterId) {
        Assert.notNull(theaterId, "theaterId can not be null");

        return seatRepository.findAllByTheaterId(theaterId)
                .stream()
                .map(ts -> TheaterDto.SeatSummary.from(ts))
                .toList();
    }

    public void saveAll(Theater theater, List<TheaterCommand.BulkAddSeat.Seat> seatList) {
        Assert.notNull(theater, "theater can not be null");
        Assert.notEmpty(seatList, "seatList can not be empty");

        List<TheaterSeat> holder = new ArrayList<>();
        final int limit = 50;
        int offset = 1;

        for (int i = 0; i < seatList.size(); i++) {
            TheaterCommand.BulkAddSeat.Seat seat = seatList.get(i);
            holder.add(TheaterSeat.create(
                    theater,
                    seat.seatRow(),
                    seat.seatColumn(),
                    seat.grade())
            );

            if (offset >= limit) {
                seatRepository.saveAll(holder);
                holder.clear();
                offset = 1;
                continue;
            }

            if (i == seatList.size() - 1) {
                seatRepository.saveAll(holder);
                holder.clear();
                break;
            }

            offset++;
        }
    }

    public void remove(Long seatId) {
        Assert.notNull(seatId, "seatId can not be null");

        if (reservationSeatRepository.existsReservedSeat(seatId)) throw new SeatInUseException();

        seatRepository.deleteById(seatId);
    }
}
