package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Theater;
import com.example.cinema_middleware.v1.repository.TheaterRepository;
import com.example.cinema_middleware.v1.service.dto.*;
import com.example.cinema_middleware.v1.service.factory.TheaterDtoFactory;
import com.example.cinema_middleware.v1.support.exception.DuplicateValueException;
import com.example.cinema_middleware.v1.support.exception.TargetNotFoundException;
import com.example.cinema_middleware.v1.support.exception.TheaterInUseException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;


@Service
@RequiredArgsConstructor
public class AdminTheaterService {

    private final TheaterRepository theaterRepository;
    private final AdminSeatService seatService;
    private final AdminScreeningService screeningService;
    private final AdminCinemaService cinemaService;

    public Page<TheaterDto.Summary> getTheaterSummary(Pageable pageable, Long cinemaId) {
        Assert.notNull(pageable, "pageable can not be null");

        return theaterRepository.findPageInCinema(pageable, cinemaId)
                .map(TheaterDtoFactory::summary);
    }

    public void addTheater(TheaterCommand.Create command) {
        Assert.notNull(command, "command can not be null");

        if (cinemaService.checkDuplicateName(command.cinemaId(), command.name())) {
            throw new DuplicateValueException();
        }

        Theater newTheater = Theater.create(
                cinemaService.getRefer(command.cinemaId()),
                command.name(), command.description()
        );

        theaterRepository.save(newTheater);
    }

    @Transactional
    public void update(Long theaterId, String name, String description) {
        Assert.notNull(theaterId, "theaterId can not be null");
        Assert.hasText(name, "name can not be null");
        Assert.hasText(description, "description can not be null");

        Theater findTheater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> { throw new TargetNotFoundException(); });

        findTheater.setName(name);
        findTheater.setDescription(description);
    }

    public void remove(Long theaterId) {
        Assert.notNull(theaterId, "theaterId can not be null");

        if (screeningService.checkHavingScreening(theaterId)) throw new TheaterInUseException();

        theaterRepository.deleteById(theaterId);
    }

    public TheaterDto.TheaterMap getAllAtTheater(Long theaterId) {
        Assert.notNull(theaterId, "theaterId can not be null");

        List<SeatDto.Summary> seatList = seatService.getAllByTheaterId(theaterId);

        if (seatList.size() == 0) throw new TargetNotFoundException();

        return new TheaterDto.TheaterMap(theaterId, seatList);
    }

    public void bulkAddSeat(TheaterCommand.BulkAddSeat command) {
        Assert.notNull(command, "bulkAddSeatCommand can not be null");

        Theater theater = theaterRepository.findById(command.theaterId())
                .orElseThrow(() -> { throw new TargetNotFoundException(); });

        seatService.saveAll(theater, command.seatList());
    }

    public void removeSeat(Long seatId) {
        Assert.notNull(seatId, "seatId can not be null");

        seatService.remove(seatId);
    }
}
