package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.*;
import com.example.cinema_middleware.v1.domain.entity.enums.*;
import com.example.cinema_middleware.v1.repository.*;
import com.example.cinema_middleware.v1.support.exception.DuplicateSeatException;
import com.example.cinema_middleware.v1.support.exception.ScreeningNotOnSaleException;
import com.example.cinema_middleware.v1.support.exception.SeatAlreadyTakenException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link MovieReservationService#addReservation} 통합 테스트.
 * 테스트마다 회원·상영관·좌석·영화·회차를 새로 만들고, 트랜잭션 롤백으로 정리한다.
 */
@SpringBootTest
@Transactional
class MovieReservationServiceTest {

    @Autowired MovieReservationService movieReservationService;
    @Autowired MovieReservationRepository movieReservationRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired CinemaRepository cinemaRepository;
    @Autowired TheaterRepository theaterRepository;
    @Autowired TheaterSeatRepository theaterSeatRepository;
    @Autowired MovieRepository movieRepository;
    @Autowired ScreeningRepository screeningRepository;
    @Autowired EntityManager em;

    private static final BigDecimal SEAT_PRICE = BigDecimal.valueOf(SeatGrade.NORMAL.getPrice());

    private Member member;
    private Theater theater;
    private Movie movie;
    private TheaterSeat seatA1;
    private TheaterSeat seatA2;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create(
                "reservation-test-" + UUID.randomUUID() + "@example.com",
                "예매테스트",
                "password",
                "01012345678",
                "19900101",
                MemberGrade.ROLE_COMMON,
                SocialProvider.NORMAL
        ));
        // AuthService 는 SecurityContext 의 principal(회원 ID)로 예매 회원을 찾는다
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(member.getId(), null, List.of()));

        theater = theaterRepository.save(Theater.create(cinemaRepository.getReferenceById(1L), "예매 테스트관", "테스트 상영관"));
        seatA1 = theaterSeatRepository.save(TheaterSeat.create(theater, "A", "1", SeatGrade.NORMAL, SEAT_PRICE));
        seatA2 = theaterSeatRepository.save(TheaterSeat.create(theater, "A", "2", SeatGrade.NORMAL, SEAT_PRICE));

        LocalDate today = LocalDate.now();
        Movie newMovie = new Movie();
        newMovie.setTitle("예매 테스트 영화");
        newMovie.setSummary("summary");
        newMovie.setRunningTime(120);
        newMovie.setAgeRating(MovieAgeRating.ONLY_ADULT);
        newMovie.setReleaseDate(today);
        newMovie.setCloseDate(today.plusDays(7));
        newMovie.setCategory(MovieCategory.ROMANCE);
        newMovie.setStatus(MovieStatus.DISPLAY);
        movie = movieRepository.save(newMovie);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Screening saveScreening(ScreeningStatus status) {
        return screeningRepository.save(Screening.create(
                movie, theater, ScreenType.V_2D, LocalDate.now().plusDays(1), LocalTime.of(10, 0), status));
    }

    @Test
    @DisplayName("예매 성공: UUID v7 ID 로 예매가 저장되고 좌석이 예매에 연결된다")
    void addReservation_success() {
        //given
        Screening screening = saveScreening(ScreeningStatus.ON_SALE);
        LocalDateTime before = LocalDateTime.now();

        //when
        UUID reservationId = movieReservationService.addReservation(
                screening.getId(), List.of(seatA1.getId(), seatA2.getId()));

        //then
        assertThat(reservationId).isNotNull();
        assertThat(reservationId.version()).isEqualTo(7);

        // 영속성 컨텍스트를 비워 DB 에 실제로 저장된 값을 읽는다
        em.flush();
        em.clear();

        MovieReservation saved = movieReservationRepository.findById(reservationId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(saved.getMember().getId()).isEqualTo(member.getId());
        assertThat(saved.getScreening().getId()).isEqualTo(screening.getId());
        assertThat(saved.getOriginalAmount()).isEqualByComparingTo(SEAT_PRICE.multiply(BigDecimal.TWO));
        assertThat(Arrays.asList(saved.getSeatNames().split(", "))).containsExactlyInAnyOrder("A1", "A2");
        assertThat(saved.getExpiredAt()).isAfter(before);

        List<MovieReservationSeat> seats = em.createQuery(
                        "select s from MovieReservationSeat s where s.reservation.id = :id", MovieReservationSeat.class)
                .setParameter("id", reservationId)
                .getResultList();
        assertThat(seats).hasSize(2);
        assertThat(seats).allSatisfy(seat -> {
            assertThat(seat.getStatus()).isEqualTo(ReservationSeatStatus.RESERVED);
            assertThat(seat.getScreening().getId()).isEqualTo(screening.getId());
        });
        assertThat(seats).extracting(seat -> seat.getTheaterSeat().getId())
                .containsExactlyInAnyOrder(seatA1.getId(), seatA2.getId());
    }

    @Test
    @DisplayName("예매 실패: 같은 회차에 이미 점유된 좌석이 포함되면 SeatAlreadyTakenException")
    void addReservation_seatAlreadyTaken() {
        //given
        Screening screening = saveScreening(ScreeningStatus.ON_SALE);
        movieReservationService.addReservation(screening.getId(), List.of(seatA1.getId()));

        //when, then
        assertThatThrownBy(() -> movieReservationService.addReservation(
                screening.getId(), List.of(seatA1.getId(), seatA2.getId())))
                .isInstanceOf(SeatAlreadyTakenException.class);
    }

    @Test
    @DisplayName("예매 성공: 다른 회차의 같은 좌석은 각각 예매할 수 있다")
    void addReservation_sameSeatOtherScreening() {
        //given
        Screening first = saveScreening(ScreeningStatus.ON_SALE);
        Screening second = screeningRepository.save(Screening.create(
                movie, theater, ScreenType.V_2D, LocalDate.now().plusDays(1), LocalTime.of(14, 0), ScreeningStatus.ON_SALE));

        //when
        UUID firstId = movieReservationService.addReservation(first.getId(), List.of(seatA1.getId()));
        UUID secondId = movieReservationService.addReservation(second.getId(), List.of(seatA1.getId()));

        //then
        assertThat(firstId).isNotEqualTo(secondId);
    }

    @Test
    @DisplayName("예매 실패: 요청에 같은 좌석이 중복되면 DuplicateSeatException")
    void addReservation_duplicateSeatInRequest() {
        //given
        Screening screening = saveScreening(ScreeningStatus.ON_SALE);

        //when, then
        assertThatThrownBy(() -> movieReservationService.addReservation(
                screening.getId(), List.of(seatA1.getId(), seatA1.getId())))
                .isInstanceOf(DuplicateSeatException.class);
    }

    @Test
    @DisplayName("예매 실패: 판매 중이 아닌 회차는 ScreeningNotOnSaleException")
    void addReservation_screeningNotOnSale() {
        //given
        Screening screening = saveScreening(ScreeningStatus.PRE_SALE);

        //when, then
        assertThatThrownBy(() -> movieReservationService.addReservation(
                screening.getId(), List.of(seatA1.getId())))
                .isInstanceOf(ScreeningNotOnSaleException.class);
    }
}
