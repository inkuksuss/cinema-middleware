# 데이터베이스 스키마

`v1.domain.entity` 패키지의 JPA 엔티티로부터 도출한 스키마 문서다.
실제 DDL은 [`schema.sql`](../src/main/resources/schema.sql)에 있으며, **이 파일이 스키마의 단일 소스**다.
테스트 데이터는 [`test-data.sql`](test-data.sql)(`scripts/load-test-data.sh`로 적재)에 있다.

> **동작**: 서버 부팅 시 Spring `spring.sql.init`(`application.yaml`)이 `src/main/resources/schema.sql`을
> 실행해 테이블을 생성하고(`spring.sql.init.mode: always`), 이후 Hibernate가 `ddl-auto: validate`로
> 엔티티 매핑과 실제 테이블을 대조한다(불일치 시 부팅 실패, 모든 `@SpringBootTest`도 실패).
>
> 모든 `CREATE`는 `CREATE TABLE IF NOT EXISTS`라 재기동은 안전하다. 다만 스키마 **변경**(컬럼 추가/타입 변경)은
> 재기동으로 반영되지 않는다. 마이그레이션 도구가 없으므로 `schema.sql`을 고친 뒤 로컬/운영 DB에
> 수동 반영하거나 `docker compose down -v`로 재생성해야 한다.
>
> 엔티티를 바꾸면 `schema.sql`도 **반드시 함께** 고쳐야 한다(그러지 않으면 validate 실패).
> 단, validate 는 컬럼 존재·타입만 본다. 길이·NULL 여부·제약·`@SQLRestriction` 의 raw SQL 은 검사하지 않으므로
> 이 부분이 어긋나도 부팅은 통과한다 — 직접 맞춰야 한다.

## 공통 규약

| 항목 | 내용 |
| --- | --- |
| 네이밍 | Spring Boot 기본 전략(CamelCase → `snake_case`) |
| PK | `{테이블}_id`, 대부분 `BIGINT AUTO_INCREMENT` (예외: `file_master.file_id`, `log_master.log_id`, 공통 코드는 문자열 PK) |
| 연관관계 | `@ManyToOne(fetch = LAZY)` → FK 컬럼 + 인덱스. 예외: `movie_reservation.movie_payment_id`(`@OneToOne`, UNIQUE) |
| Enum | `@Enumerated(STRING)` → `VARCHAR(30)` |
| 금액 | `DECIMAL(10, 2)` |
| 일시 | `DATETIME(6)` (`LocalDateTime`), 날짜 `DATE`, 시각 `TIME(6)` |
| 문자셋 | `utf8mb4` / `utf8mb4_0900_ai_ci`, InnoDB |

### 감사(audit) 컬럼

| 상속 클래스 | 컬럼 |
| --- | --- |
| `TimeEntity` | `created_at`, `updated_at` |
| `DefaultEntity` (`TimeEntity` 상속) | `created_at`, `updated_at`, `created_by`, `updated_by` |
| `HistoryEntity` | `change_type`, `change_reason`, `history_created_at`, `history_created_by` |

> `created_by` / `updated_by` / `history_created_by`는 `AuditorAware` 빈이 아직 미등록이라 현재 `NULL`로만 채워진다.
> base 클래스에 `is_delete` 는 없다 — soft delete 는 엔티티마다 따로 선언한다(아래).

### Soft delete

세 가지 방식이 섞여 있다.

| 방식 | 대상 | 매핑 |
| --- | --- | --- |
| `is_delete BIT(1) DEFAULT b'0'` | `coupon`, `file_master`, `common_code_group`, `common_code_detail` | `@SQLRestriction("is_delete = false")` + `@SQLDelete(... SET is_delete = true ...)` |
| `status` enum 값 | `member`(`'DELETE'`), `movie`(`'HIDDEN'`), `screening`(`'DELETE'`), `cinema`(`'DELETE'`), `theater`(`'DELETE'`), `theater_seat`(`'DELETE'`) | `@SQLRestriction("status <> 'DELETE'")` 등 + `@SQLDelete(... SET status = ...)` |
| 없음(물리 삭제) | `movie_reservation`, `movie_reservation_seat`, `movie_payment`, `coupon_issuance`, `movie_reservation_history`, `movie_payment_history`, `log_master` | - |

`repository.delete(...)` 는 soft delete 대상이면 실제로 UPDATE 되고, 조회에는 `@SQLRestriction` 조건이 자동으로 붙는다.

## 테이블 목록

| 테이블 | 설명 | base | 주요 FK |
| --- | --- | --- | --- |
| `member` | 회원 | `TimeEntity` | - |
| `movie` | 영화 | `DefaultEntity` | - |
| `cinema` | 영화관(지점) | `DefaultEntity` | - |
| `theater` | 상영관 | `DefaultEntity` | - |
| `theater_seat` | 상영관 좌석 | `DefaultEntity` | `theater` |
| `screening` | 상영 회차 | `DefaultEntity` | `movie`, `theater` |
| `movie_reservation` | 영화 예매 | `DefaultEntity` | `member`, `screening`, `coupon_issuance`(nullable), `movie_payment`(nullable, 확정 결제) |
| `movie_reservation_seat` | 예매 좌석(홀드/점유) | `DefaultEntity` | `movie_reservation`, `screening`, `theater_seat` |
| `movie_payment` | 결제 시도 | `DefaultEntity` | `movie_reservation` |
| `movie_reservation_history` | 예매 변경 이력(스냅샷) | `HistoryEntity` | - (참조 ID 값만 보관, 물리 FK 없음) |
| `movie_payment_history` | 결제 시도 변경 이력(스냅샷) | `HistoryEntity` | - (참조 ID 값만 보관, 물리 FK 없음) |
| `coupon` | 쿠폰 정의 | `DefaultEntity` | - |
| `coupon_issuance` | 쿠폰 발급 내역 | `DefaultEntity` | `member`, `coupon` |
| `file_master` | 첨부 파일(포스터 등) | `DefaultEntity` | - (`target_id`+`target_type` polymorphic 참조, 물리 FK 없음) |
| `log_master` | 변경 이력 로그 | 없음(독자 필드) | - (대상 레코드 식별 컬럼 없음) |
| `common_code_group` | 공통 코드 그룹 | `DefaultEntity` | - (PK = 문자열 코드) |
| `common_code_detail` | 공통 코드 상세 | `DefaultEntity` | `common_code_group` (복합 PK) |

## 주요 Enum 값

| 컬럼 | Enum | 값 |
| --- | --- | --- |
| `member.status` | `MemberStatus` | `ACTIVE`, `SLEEP`, `LOCKED`, `DELETE`(soft delete) |
| `member.grade` | `MemberGrade` | `ROLE_COMMON`, `ROLE_VIP`, `ROLE_MANAGE_ADMIN`, `ROLE_SUPER_ADMIN` |
| `movie.status` | `MovieStatus` | `DISPLAY`, `HIDDEN`(soft delete) |
| `cinema.status` | `CinemaStatus` | `OPEN`, `CLOSE`, `DELETE`(soft delete) |
| `theater.status` | `TheaterStatus` | `AVAILABLE`, `UNAVAILABLE`, `DELETE`(soft delete) |
| `theater_seat.status` | `SeatStatus` | `AVAILABLE`, `UNAVAILABLE`, `DELETE`(soft delete) |
| `theater_seat.grade` | `SeatGrade` | `NORMAL`, `ADVANCED` |
| `screening.status` | `ScreeningStatus` | `PRE_SALE`, `ON_SALE`, `END_SALE`, `SOLD_OUT`, `DELETE`(soft delete) |
| `screening.screen_type` | `ScreenType` | `V_2D`, `V_4D`, `V_SCREENX` (DB 에는 키 저장, 응답은 `displayName` 으로 변환) |
| `movie_reservation.status` | `ReservationStatus` | `PENDING`, `SUCCESS`, `EXPIRED`, `CANCELED` |
| `movie_reservation_seat.status` | `ReservationSeatStatus` | `RESERVED`, `NULL`(취소/만료) |
| `movie_payment.status` | `PaymentStatus` | `PENDING`, `COMPLETE`, `FAILED`, `CANCELED` |
| `movie_payment.method` | `PaymentMethod` | `CARD`, `KAKAO`, `TOSS` (`movie_payment_history.method` 는 문자열 `VARCHAR(100)`) |
| `coupon.status` | `CouponStatus` | `PRE_ISSUE`, `CAN_ISSUE`, `SOLD_OUT`, `DELETE` |
| `coupon.usage_type` | `CouponUsage` | `MOVIE`, `PRODUCT` |
| `coupon.discount_type` | `DiscountType` | `PERCENT_10`, `PERCENT_50`, `MINUS_5000`, `MINUS_3000` |
| `file_master.usage_type` | `FileUsage` | `THUMBNAIL`, `IMAGE` |
| `coupon_issuance.status` | `IssuanceStatus` | `USED`, `ISSUED`, `EXPIRED` |

## 관계도

```
member ─┬─< movie_reservation >─┬─ screening ─┬─< movie_reservation_seat
        │                       │             │
        │                       ├─ coupon_issuance (nullable, 예매당 최대 1개)
        │                       │             │
        │                       │             └─ (theater_seat)
        └─< coupon_issuance >─ coupon          │
                                               │
movie ──< screening                            │
                                               │
theater ─┬─< theater_seat ─────────────────────┘
         └─< screening

movie_reservation ──< movie_reservation_seat
movie_reservation ──< movie_payment   (결제 시도, N)
movie_reservation ─── movie_payment   (확정 결제, 1:1 · movie_reservation.movie_payment_id)

movie_reservation ··· movie_reservation_history   (스냅샷, FK 없음)
movie_reservation ··· movie_payment_history       (스냅샷, FK 없음)

common_code_group ──< common_code_detail
```

## 특이 사항

### `movie_reservation_seat` — 좌석 중복 점유 방지

```sql
UNIQUE KEY uk_seat_hold (screening_id, theater_seat_id, status)
```

- 한 상영 회차의 한 좌석은 **점유(`status='RESERVED'`) 상태로 하나만** 존재 가능 → 동시 예매 시 DB 유니크 제약으로 방어.
- 취소/만료 시 애플리케이션이 `status`를 `NULL`로 변경 (`MovieReservationSeat.giveUpSeat()`).
  MySQL은 유니크 인덱스에서 `NULL`을 서로 다른 값으로 취급하므로 과거 점유 이력은 제약에 걸리지 않는다.
  그래서 `status` 는 NOT NULL 이 아니다.
- `idx_mrs_screening` 은 `fk_mrs_screening` 전용 인덱스다. `uk_seat_hold` 선두 컬럼이 `screening_id` 라
  FK 가 그 인덱스에 의존하면 `uk_seat_hold` 를 교체할 때 드롭이 막히므로 별도로 둔다.
- 좌석 가격 컬럼은 이 테이블에도, `theater_seat`에도 없다 (아래 참고).

### 좌석 가격 — 컬럼이 아닌 `SeatGrade`

`theater_seat.price` 컬럼은 제거됐다. 가격은 `SeatGrade` enum 이 들고 있다(`NORMAL → 0`, `ADVANCED → 5000`).
`movie_reservation.total_price` 는 예매 시점에 계산돼 그대로 저장되는 값이다.

### `usage_type` — 예약어 회피

`usage` 는 MySQL 예약어다. 엔티티 필드명이 `usage` 인 `coupon`, `file_master` 는 컬럼명을 `usage_type` 으로 둔다.
Hibernate 는 식별자를 인용하지 않으므로 엔티티에서 `@Column(name = "usage_type")` 으로 명시해야 한다.

### `screening.screen_type` — 키 저장, 표시명 변환

DB 에는 `ScreenType` 의 키(`V_2D` 등)를 `VARCHAR(30)` 으로 저장하고, 응답 DTO 를 만들 때 `displayName`(`2D` 등)으로 변환한다.

### `movie_reservation.expired_at` — 홀드 만료

**NOT NULL, `updatable = false`**. `MovieReservation.create()` 가 생성 시각 + 10분으로 정하고 이후 바뀌지 않는다
(확정돼도 null 로 지우지 않는다). 예매에 `canceled_at` 컬럼은 없다.

### `movie_reservation.coupon_issuance` — 예매에 적용한 쿠폰

`@ManyToOne(fetch = LAZY)` `@JoinColumn(name = "coupon_issuance_id")`, **nullable**
(쿠폰 미사용 예매 허용). `@OneToOne`이 아니라 `@ManyToOne`이므로 DB 유니크 제약은 없다
(같은 발급건이 여러 예매에 연결되는 것을 스키마가 막지는 않음 — 애플리케이션 책임).
`coupon_issuance` 테이블이 `schema.sql`에서 `movie_reservation`보다 뒤에 정의되지만,
파일 상단 `SET FOREIGN_KEY_CHECKS = 0` 덕에 순서와 무관하게 FK가 생성된다.

### `movie_reservation` ↔ `movie_payment` — 결제 시도(1:N)와 확정 결제(1:1)

서로 다른 FK 컬럼을 쓰는 **독립된 단방향 연관관계 두 개**다(`mappedBy` 없음, 순환 FK).

| 연관관계 | FK 컬럼 | 의미 |
| --- | --- | --- |
| `MoviePayment.reservation` (`@ManyToOne`, NOT NULL) | `movie_payment.movie_reservation_id` | 시도가 **속한** 예매 |
| `MovieReservation.payment` (`@OneToOne`, nullable) | `movie_reservation.movie_payment_id` (UNIQUE) | 예매를 **확정한** 결제 |

- 결제 시도 1회 = `movie_payment` 행 1개. 재시도는 새 멱등키로 새 행을 만든다. 부분 결제는 지원하지 않는다
  (`completePayment`가 `paid_amount = total_price` 를 검증).
- 예매 조회 시 `mr.payment` 하나만 fetch join 하면 되고, 유효 결제를 골라내는 필터링이 없다.
  결제 시도 목록은 예매에 컬렉션으로 두지 않는다.
- 생성 순서: 예매 INSERT → 결제 INSERT → 예매 UPDATE(`movie_payment_id`). 예매 쪽 FK 가 nullable 이라 순환 FK 가 풀린다.
- 결제 완료·취소 시각은 `movie_payment.paid_at` / `canceled_at` 에 둔다.
- **멱등성**
  - 같은 시도의 중복 요청: `movie_payment.idempotency_key` UNIQUE (PG orderId).
  - 이미 결제된 예매에 새 시도: `MovieReservation.startPayment` 가 `payment != null` 또는 `status != PENDING` 이면 거부.
  - 서로 다른 시도의 동시 승인: `movie_reservation.version`(`@Version`) 낙관적 락 + `movie_payment_id` UNIQUE.
    진 쪽은 롤백되지만 PG 승인은 이미 났으므로 **PG 취소 보상이 필요**하다.
- `movie_payment_id` UNIQUE 는 "결제 하나를 예매 여러 개가 가리키는 것"만 막는다.
  "다른 예매의 결제를 가리키는 것"은 막지 못하므로 `completePayment` 가 `payment.reservation.id == this.id` 를 검증한다.

### `movie_reservation_history` / `movie_payment_history` — 스냅샷 이력

- 변경 시점의 값을 복사해 남기는 append-only 테이블이다(`HistoryEntity` 상속, `updated_*` 없음).
- 원본 생명주기와 독립적이어야 하므로 연관관계 없이 참조 ID 를 값(`BIGINT`)으로만 보관하고
  물리 FK 는 두지 않는다(조회용 인덱스만). 두 테이블 모두 예매 참조 컬럼명은 `movie_reservation_id` 다.
- `movie_payment_history.idempotency_key` 는 시도 하나에 이력이 여러 행 쌓이므로 UNIQUE 가 아닌 일반 인덱스다.
- `movie_reservation_history.canceled_at` 은 컬럼이 남아 있지만 원본(예매)에 값이 없어 현재 항상 `NULL` 이다.

### `common_code_detail` — 복합 PK

`@EmbeddedId`(`CommonCodeDetailId`)로 `(common_code_group_id, common_code_detail_code)` 복합 키를 쓴다.
`common_code_group_id`는 PK인 동시에 `common_code_group`으로의 FK(`@MapsId`)다.
`display_name`(NOT NULL)은 화면 표시명이고, `description`은 부가 설명이다.

### `movie.description` / `log_master.body`

`@Lob String` → MySQL에서 `LONGTEXT`.
