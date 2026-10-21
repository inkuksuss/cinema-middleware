# 데이터베이스 스키마

`v1.domain.entity` 패키지의 JPA 엔티티로부터 도출한 스키마 문서다.
실제 DDL은 [`schema.sql`](../src/main/resources/schema.sql)에 있으며, **이 파일이 스키마의 단일 소스**다.

> **동작**: 서버 부팅 시 Spring `spring.sql.init`(`application.yaml`)이 `src/main/resources/schema.sql`을
> 실행해 테이블을 생성하고(`spring.sql.init.mode: always`), 이후 Hibernate가 `ddl-auto: validate`로
> 엔티티 매핑과 실제 테이블을 대조한다(불일치 시 부팅 실패, 모든 `@SpringBootTest`도 실패).
>
> 모든 `CREATE`는 `CREATE TABLE IF NOT EXISTS`라 재기동은 안전하다. 다만 스키마 **변경**(컬럼 추가/타입 변경)은
> 재기동으로 반영되지 않는다. 마이그레이션 도구가 없으므로 `schema.sql`을 고친 뒤 로컬/운영 DB에
> 수동 반영하거나 `docker compose down -v`로 재생성해야 한다.
>
> 엔티티를 바꾸면 `schema.sql`도 **반드시 함께** 고쳐야 한다(그러지 않으면 validate 실패).

## 공통 규약

| 항목 | 내용 |
| --- | --- |
| 네이밍 | Spring Boot 기본 전략(CamelCase → `snake_case`) |
| PK | `{테이블}_id`, 대부분 `BIGINT AUTO_INCREMENT` |
| 연관관계 | 전부 `@ManyToOne(fetch = LAZY)` → FK 컬럼 + 인덱스 |
| Enum | `@Enumerated(STRING)` → `VARCHAR(100)` (예외: `coupon_issuance.status`는 애너테이션 없어 ORDINAL 정수) |
| Soft delete | `is_delete CHAR(1) DEFAULT 'N'`. `delete` 호출 시 `UPDATE ... SET is_delete='Y'`, 조회 시 `is_delete='N'` 자동 필터 |
| 금액 | `DECIMAL(10, 2)` |
| 일시 | `DATETIME(6)` (`LocalDateTime`), 날짜 `DATE`, 시각 `TIME(6)` |

### 감사(audit) 컬럼

| 상속 클래스 | 컬럼 |
| --- | --- |
| `BaseEntity` | `created_at`, `updated_at`, `created_by`, `updated_by`, `is_delete` |
| `SimpleBaseEntity` | `created_at`, `updated_at`, `is_delete` |

> `created_by` / `updated_by`는 `AuditorAware` 빈이 아직 미등록이라 현재 `NULL`로만 채워진다.

## 테이블 목록

| 테이블 | 설명 | base | 주요 FK |
| --- | --- | --- | --- |
| `member` | 회원 | `SimpleBaseEntity` | - |
| `movie` | 영화 | `BaseEntity` | - |
| `theater` | 상영관 | `BaseEntity` | - |
| `theater_seat` | 상영관 좌석 (좌석 가격 포함) | `BaseEntity` | `theater` |
| `screening` | 상영 회차 | `BaseEntity` | `movie`, `theater` |
| `movie_reservation` | 영화 예매 | `BaseEntity` | `member`, `screening`, `coupon_issuance`(nullable) |
| `movie_reservation_seat` | 예매 좌석(홀드/점유) | `SimpleBaseEntity` | `movie_reservation`, `screening`, `theater_seat` |
| `movie_payment` | 결제 (TODO, 골격만) | `BaseEntity` | `movie_reservation` |
| `coupon` | 쿠폰 정의 | `BaseEntity` | - |
| `coupon_issuance` | 쿠폰 발급 내역 | `SimpleBaseEntity` | `member`, `coupon` |
| `file_master` | 첨부 파일(포스터 등) | `BaseEntity` | - (`target_id`+`target_type` polymorphic 참조, 물리 FK 없음) |
| `log_master` | 변경 이력 로그 | 없음(독자 필드) | - (대상 레코드 식별 컬럼 없음) |
| `common_code_group` | 공통 코드 그룹 | `BaseEntity` | - (PK = 문자열 코드) |
| `common_code_detail` | 공통 코드 상세 | `BaseEntity` | `common_code_group` (복합 PK) |

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
movie_reservation ──< movie_payment

common_code_group ──< common_code_detail
```

## 특이 사항

### `movie_reservation_seat` — 좌석 중복 점유 방지

```sql
UNIQUE KEY uk_seat_hold (screening_id, theater_seat_id, is_active)
```

- 한 상영 회차의 한 좌석은 **활성(`is_active='Y'`) 상태로 하나만** 존재 가능 → 동시 예매 시 DB 유니크 제약으로 방어.
- 취소/만료 시 애플리케이션이 `is_active`를 `NULL`로 변경 (`MovieReservationSeat.changeIsActive()`).
  MySQL은 유니크 인덱스에서 `NULL`을 서로 다른 값으로 취급하므로 과거 점유 이력은 제약에 걸리지 않는다.
- 엔티티 필드 타입이 `String`이라 컬럼도 `VARCHAR`다. (`CHAR(1)`이 더 적합하지만 매핑을 따름)
- 좌석 가격은 이 테이블이 아니라 **`theater_seat.price`**에 있다 (아래 참고).

### `theater_seat.price` — 좌석 가격의 귀속

좌석 가격은 회차나 예매가 아니라 **좌석 자체**에 귀속된다. `TheaterSeat.of()` 팩토리가
등급별로 정한다: `NORMAL → 8000`, `ADVANCED → 10000`.
과거 `movie_reservation_seat.price`에 있던 것을 여기로 이관했다.
예매 총액(`movie_reservation.total_price`)은 예매된 좌석들의 `theater_seat.price` 합계다.

### `movie_reservation.coupon_issuance` — 예매에 적용한 쿠폰

`@ManyToOne(fetch = LAZY)` `@JoinColumn(name = "coupon_issuance_id")`, **nullable**
(쿠폰 미사용 예매 허용). `@OneToOne`이 아니라 `@ManyToOne`이므로 DB 유니크 제약은 없다
(같은 발급건이 여러 예매에 연결되는 것을 스키마가 막지는 않음 — 애플리케이션 책임).
`coupon_issuance` 테이블이 `schema.sql`에서 `movie_reservation`보다 뒤에 정의되지만,
파일 상단 `SET FOREIGN_KEY_CHECKS = 0` 덕에 순서와 무관하게 FK가 생성된다.

### `coupon_issuance.status` — ORDINAL 저장

`CouponIssuance.status` 필드에 `@Enumerated`가 없어 JPA 기본값인 **ORDINAL(정수)**로 저장된다.
`IssuanceStatus` enum 순서 기준: `USED=0`, `ISSUED=1`, `EXPIRED=2`.
(다른 enum 컬럼은 모두 `@Enumerated(STRING)`이라 문자열로 저장됨 — 일관성 관점에서 향후 정리 대상)

### `common_code_detail` — 복합 PK

`@EmbeddedId`(`CommonCodeDetailId`)로 `(common_code_group_id, common_code_detail_code)` 복합 키를 쓴다.
`common_code_group_id`는 PK인 동시에 `common_code_group`으로의 FK(`@MapsId`)다.

### `movie.description`

`@Lob String` → MySQL에서 `LONGTEXT`.
