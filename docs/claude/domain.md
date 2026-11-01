# 엔티티 · 리포지토리

## 베이스 클래스 (`domain/entity/base`)

| 클래스 | 필드 | 사용 엔티티 |
| --- | --- | --- |
| `TimeEntity` | `createdAt`, `updatedAt` | `Member` |
| `DefaultEntity` extends `TimeEntity` | + `createdBy`, `updatedBy`(Long) | 대부분 |
| `HistoryEntity` | `changeType`, `changeReason`, `historyCreatedAt`, `historyCreatedBy` | `*History` |
| (상속 없음) | — | `LogMaster` |

- `@EnableJpaAuditing` 은 `CinemaMiddlewareApplication` 에 있다. `AuditorAware` 빈은 없어
  `createdBy` / `updatedBy` / `historyCreatedBy` 는 자동으로 채워지지 않는다.

## Soft delete — 엔티티마다 방식이 다르다

| 방식 | 엔티티 |
| --- | --- |
| `status` 값 (`@SQLRestriction` + `@SQLDelete`) | `Member`(`DELETE`), `Movie`(`HIDDEN`), `Cinema`·`Theater`·`TheaterSeat`·`Screening`(`DELETE`) |
| `is_delete` boolean (`is_delete = false`) | `Coupon`, `FileMaster`, `CommonCodeGroup`, `CommonCodeDetail` |
| 없음 (물리 삭제) | `MovieReservation`, `MovieReservationSeat`, `MoviePayment`, `CouponIssuance`, `*History`, `LogMaster` |

`repository.delete(...)` 는 soft delete 대상이면 UPDATE 로 실행되고, 조회에는 `@SQLRestriction` 조건이 자동으로 붙는다.
`Movie` 는 `HIDDEN` 이 곧 soft delete 라 숨긴 영화는 JPA 조회에서 빠진다.

## 매핑 규약 (현재 코드)

- PK 컬럼 `{테이블}_id`, `@GeneratedValue(IDENTITY)`. `CommonCodeDetail` 만 `@EmbeddedId`(`key/CommonCodeDetailId`).
- 연관관계는 `@ManyToOne(LAZY)` 가 기본. `MovieReservation.payment` 만 `@OneToOne(LAZY)`.
  역방향 `@OneToMany(mappedBy)` 는 `CommonCodeGroup`, `MovieReservation`, `TheaterSeat` 에 있다.
- enum 은 전부 `@Enumerated(STRING)`. `ScreenType` 은 키(`V_2D` 등)를 저장하고 응답에는 `displayName` 을 쓴다.
- `MovieReservation` 에 `@Version`(낙관적 락)이 있다.
- 기본 생성자는 `@NoArgsConstructor(access = PUBLIC)` 이 대부분이고,
  `MoviePayment`, `MovieReservationHistory`, `MoviePaymentHistory` 만 `PROTECTED` 다.
- 생성은 엔티티의 `static create(...)` 팩토리(Spring `Assert` 로 필수값 검증)를 쓰는 곳이 많다.
  상태 변경은 도메인 메서드(`confirm()`, `completePayment()`, `approve()` 등)와 setter(`Movie`, `Theater`,
  `Screening`, `FileMaster`, `MovieReservationSeat`)가 섞여 있다.

## 도메인 맵

- 회원: `Member` — `MemberGrade`(ROLE_COMMON/VIP/MANAGE_ADMIN/SUPER_ADMIN), `MemberStatus`, `SocialProvider`.
- 상영: `Cinema` → `Theater` → `TheaterSeat`(`SeatGrade`, `price`, `SeatStatus`).
  `Movie` + `Theater` → `Screening`(`ScreenType`, `startDate`, `startAt`, `ScreeningStatus`).
- 예매: `MovieReservation`(member, screening, couponIssuance, payment, `code`, `seatNames`, `originalAmount`,
  `ReservationStatus`, `expiredAt`) → `MovieReservationSeat`(screening, theaterSeat, `status`).
  - `movie_reservation_seat.status` 는 `RESERVED` 또는 NULL(해제)이며, 유니크 키
    `(screening_id, theater_seat_id, status)` 로 같은 회차 좌석의 중복 점유를 막는다.
  - `seatNames` 는 `", "` 로 이어 붙인 좌석명이다(`getSeatCount()` 가 이 구분자로 센다).
- 결제: `MoviePayment` — 예매 1 : 결제 시도 N(`idempotencyKey` 유니크). 확정 결제는 `MovieReservation.payment`.
- 이력: `MovieReservationHistory`, `MoviePaymentHistory`(스냅샷, 연관관계 없음).
- 쿠폰: `Coupon`(`CouponUsage`, `DiscountType`, `CouponStatus`) / `CouponIssuance`(`IssuanceStatus`).
- 기타: `FileMaster`(`TableType` + targetId 다형 참조), `LogMaster`, `CommonCodeGroup` / `CommonCodeDetail`.

## 리포지토리

- QueryDSL 3분할 패턴: `XxxRepository`(JpaRepository + `XxxRepositoryQuery`) / `XxxRepositoryQuery`(시그니처) /
  `XxxRepositoryImpl`(구현, 이름이 `Impl` 이어야 Spring Data 가 합친다).
  적용: Cinema, Member, Movie, MovieReservation, ReservationSeat, Screening, Theater, TheaterSeat.
- `JPAQueryFactory` 는 빈이 아니라 각 `Impl` 생성자에서 `EntityManager` 로 만든다.
- 조회 결과는 `Projections.constructor` 로 `repository/dao/XxxDao.*` record 에 담는다.
- `AuthTokenRepository` 는 JPA 가 아니라 Redis 전담 리포지토리다 → auth.md.
- `Q*` 클래스는 `querydsl-apt`(jpa)가 컴파일 시 `build/generated/` 에 만든다.
