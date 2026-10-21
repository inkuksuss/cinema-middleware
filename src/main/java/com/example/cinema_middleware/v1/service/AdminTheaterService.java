package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.Theater;
import com.example.cinema_middleware.v1.repository.TheaterRepository;
import com.example.cinema_middleware.v1.service.dto.*;
import com.example.cinema_middleware.v1.service.dto.forms.BulkAddSeatForm;
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

    public Page<TheaterSummary> getTheaterSummary(Pageable pageable) {
        Assert.notNull(pageable, "pageable can not be null");

        return theaterRepository.findTheaterPage(pageable)
                .map(t -> TheaterSummary.from(t));
    }

    public void addTheater(String name, String description) {
        Assert.hasText(name, "name can not be null");
        Assert.hasText(description, "description can not be null");

        theaterRepository.findByName(name)
                         .ifPresent(t -> { throw new DuplicateValueException("이미 존재하는 상영관 명입니다."); });

        theaterRepository.save(Theater.create(name, description));
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

        if (screeningService.isUsing(theaterId)) throw new TheaterInUseException();

        theaterRepository.deleteById(theaterId);
    }

    public TheaterSeatMapper getAllAtTheater(Long theaterId) {
        Assert.notNull(theaterId, "theaterId can not be null");

        List<AdminSeatSummary> seatList = seatService.getAllByTheaterId(theaterId);

        return new TheaterSeatMapper(theaterId, seatList);
    }

    public void bulkAddSeat(BulkAddSeatForm form) {
        Assert.notNull(form, "bulkAddSeatForm can not be null");

        Theater theater = theaterRepository.getReferenceById(form.getTheaterId());

        seatService.saveAll(theater, form.getSeatList());
    }

    public void removeSeat(Long seatId) {
        Assert.notNull(seatId, "seatId can not be null");

        seatService.remove(seatId);
    }
}
