package com.example.cinema_middleware.v1.service.factory;

import com.example.cinema_middleware.v1.repository.dao.ScreeningDao;
import com.example.cinema_middleware.v1.service.dto.ScreeningDto;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상영 응답 DTO 조립기.
 * 엔티티·조회 결과(repository.dto) → {@link ScreeningDto} 변환의 유일한 지점이다.
 */
public final class ScreeningDtoFactory {

    private ScreeningDtoFactory() {
    }

    /** 상영 목록과 좌석 수 목록을 screeningId 로 짝지어 사용자 상영 목록을 만든다. */
    public static List<ScreeningDto.Summary> summaryList(List<ScreeningDao.Preview> previewList) {
        Assert.notNull(previewList, "infoList can not be null");

        return previewList.stream()
                .map(preview -> summary(preview))
                .toList();
    }

    private static ScreeningDto.Summary summary(ScreeningDao.Preview preview) {
        Assert.notNull(preview, "info can not be null");

        return new ScreeningDto.Summary(
                preview.id(),
                LocalDateTime.of(preview.startDate(), preview.startAt()),
                preview.type(),
                preview.status(),

                preview.movieId(),
                preview.movieTitle(),
                preview.movieRunningTime(),
                preview.movieAgeRating(),
                preview.moviePosterUrl(),

                preview.theaterId(),
                preview.theaterName(),

                preview.cinemaId(),
                preview.cinemaName(),

                preview.totalSeatCount(),
                preview.totalSeatCount() - preview.reservedSeatCount()
        );
    }

    public static ScreeningDto.AdminSummary adminSummary(ScreeningDao.AdminRow row) {
        Assert.notNull(row, "row can not be null");

        return new ScreeningDto.AdminSummary(
                row.screeningId(),
                row.startDate(),
                row.startAt(),
                row.status(),
                row.screenType(),

                row.movieId(),
                row.movieTitle(),
                row.movieRunningTime(),
                row.movieAgeRating(),

                row.theaterId(),
                row.theaterName(),

                row.cinemaId(),
                row.cinemaName()
        );
    }

    public static ScreeningDto.AdminDetail adminDetail(ScreeningDao.AdminRowDetail detail) {
        Assert.notNull(detail, "detail can not be null");

        return new ScreeningDto.AdminDetail(
                detail.screeningId(),
                detail.startDate(),
                detail.startAt(),
                detail.status(),
                detail.screenType(),
                detail.createdAt(),
                detail.updatedAt(),
                detail.createdBy(),
                detail.updatedBy(),

                detail.movieId(),
                detail.movieTitle(),
                detail.movieRunningTime(),
                detail.movieAgeRating(),
                detail.movieCategory(),

                detail.theaterId(),
                detail.theaterName(),

                detail.cinemaId(),
                detail.cinemaName()
        );
    }
}
