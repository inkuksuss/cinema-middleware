package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.FileMaster;
import com.example.cinema_middleware.v1.domain.entity.Movie;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;

/**
 * 영화 도메인 서비스 응답 DTO 모음.
 *
 * <p>응답은 중첩 없는 평탄 구조다. 다른 도메인에서 온 필드는 출처를 드러내도록
 * 도메인 접두사를 붙인다. 단 컬렉션 필드(1:N)는 스칼라로 펼칠 수 없으므로
 * 평탄화 대상이 아니다({@code Detail.fileList}).
 *
 * <p>관리자/사용자 응답 필드가 동일해 하나를 공유한다.
 * 상위 계층(controller)을 참조하지 않는다.
 */
public final class MovieDto {

    private MovieDto() {
    }

    /** 목록 조회용 요약 */
    public record Summary(
            Long id,
            String title,
            String summary,
            Integer runningTime,
            MovieAgeRating ageRating,
            LocalDate releaseDate,
            Long viewCount,
            MovieCategory category,
            String posterUrl
    ) {
        public static Summary from(Movie movie) {
            Assert.notNull(movie, "movie can not be null");

            return new Summary(
                    movie.getId(),
                    movie.getTitle(),
                    movie.getSummary(),
                    movie.getRunningTime(),
                    movie.getAgeRating(),
                    movie.getReleaseDate(),
                    movie.getViewCount(),
                    movie.getCategory(),
                    movie.getPosterUrl()
            );
        }

        /**
         * 조회 결과(row)처럼 엔티티 없이 식별 정보만 있을 때 사용.
         * 현재 미사용 — ScreeningDto 평탄화로 호출처가 사라졌다.
         */
        public static Summary of(Long id, String title) {
            return new Summary(id, title, null, null, null, null, null, null, null);
        }
    }

    /** 상세 조회 — 첨부파일 목록 포함 */
    public record Detail(
            Long id,
            String title,
            String summary,
            String description,
            Integer runningTime,
            MovieAgeRating ageRating,
            LocalDate releaseDate,
            LocalDate closeDate,
            String posterUrl,
            Long viewCount,
            MovieCategory category,
            List<FileDto.Summary> fileList
    ) {
        public static Detail from(Movie movie) {
            return of(movie, List.of());
        }

        public static Detail of(Movie movie, List<FileMaster> fileMasterList) {
            Assert.notNull(movie, "movie can not be null");
            Assert.notNull(fileMasterList, "fileMasterList can not be null");

            return new Detail(
                    movie.getId(),
                    movie.getTitle(),
                    movie.getSummary(),
                    movie.getDescription(),
                    movie.getRunningTime(),
                    movie.getAgeRating(),
                    movie.getReleaseDate(),
                    movie.getCloseDate(),
                    movie.getPosterUrl(),
                    movie.getViewCount(),
                    movie.getCategory(),
                    fileMasterList.stream().map(FileDto.Summary::from).toList()
            );
        }
    }
}
