-- =============================================================================
-- cinema-middleware 스키마 DDL
-- 대상 DBMS: MySQL 8.4
-- 문자셋: utf8mb4 / 정렬: utf8mb4_0900_ai_ci
--
-- 엔티티(`v1.domain.entity`) 정의로부터 도출한 테이블 생성 스크립트다.
-- Spring Boot 기본 네이밍 전략(CamelCase -> snake_case)을 적용했다.
--
-- 이 파일이 스키마의 단일 소스다. 서버 부팅 시 Spring `spring.sql.init` 이
-- 이 스크립트를 실행해 테이블을 생성하고, 이후 Hibernate 가 `ddl-auto: validate`
-- 로 엔티티 매핑과 실제 테이블을 대조한다(불일치 시 부팅 실패).
--   - 모든 CREATE 는 `CREATE TABLE IF NOT EXISTS` 라 재기동 시 에러 없이 통과한다.
--   - 다만 스키마 "변경"(컬럼 추가/타입 변경)은 재기동으로 반영되지 않는다.
--     마이그레이션 도구가 없으므로 이 파일을 고친 뒤 로컬/운영 DB 에
--     수동으로 반영하거나 `docker compose down -v` 로 재생성해야 한다.
--
-- 규약
--   - 감사(audit) 컬럼은 상속하는 base 클래스로 결정된다:
--       DefaultEntity -> created_at, updated_at, created_by, updated_by
--       TimeEntity    -> created_at, updated_at
--       HistoryEntity -> change_type, change_reason,
--                        history_created_at, history_created_by
--     (상속 없음: log_master — append-only)
--   - Soft delete 는 두 가지 방식이 혼재한다:
--       (a) is_delete BIT(1) DEFAULT b'0' 컬럼
--           -> @SQLRestriction("is_delete = false")
--              + @SQLDelete(... SET is_delete = true ...)
--           대상: member, theater, theater_seat, coupon,
--                 file_master, common_code_group, common_code_detail
--       (b) status 컬럼의 특정 enum 값으로 대체 (is_delete 컬럼 없음)
--           -> movie: status='HIDDEN' / screening: status='DELETE', coupon_issuance
--       (c) soft delete 없음 (물리 삭제)
--           -> movie_reservation, movie_reservation_seat, movie_payment,
--              movie_reservation_history, log_master
--   - PK 컬럼명은 `{테이블}_id`, 대부분 BIGINT AUTO_INCREMENT.
--   - 연관관계는 전부 @ManyToOne(LAZY) -> 물리적으로 FK 컬럼 + 인덱스.
--     예외: file_master 는 target_id+target_type(enum) 으로 여러 테이블을 가리키는
--     polymorphic 참조라 물리적 FK 가 없다(조회용 인덱스만).
--     예외: movie_reservation_history 는 스냅샷 테이블이라 연관관계 없이
--     참조 ID 를 값으로만 보관한다(물리적 FK 없음, 조회용 인덱스만).
--   - Enum 은 @Enumerated(STRING) -> VARCHAR(100) 로 저장.
--     (CommonCode 계열은 문자열 코드를 그대로 PK 로 쓴다)
--   - 예매 : 결제 = 1:N (부분결제·재결제·환불을 결제 행으로 쌓는다).
--     FK 는 movie_payment.movie_reservation_id 한쪽에만 둔다(유니크 제약 없음).
--
-- 실행 순서: 참조 무결성 때문에 아래 정의 순서대로 생성해야 한다.
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- member : 회원
--   base: TimeEntity (created_by/updated_by 없음)
--   soft delete: is_delete BIT
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS member
(
    member_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원 PK',
    email           VARCHAR(255) NOT NULL COMMENT '이메일(로그인 ID)',
    username        VARCHAR(100) NOT NULL COMMENT '회원 이름',
    password        VARCHAR(255) NOT NULL COMMENT 'BCrypt 해시 비밀번호',
    phone_number    VARCHAR(100) NOT NULL COMMENT '휴대전화 번호',
    birthday        VARCHAR(100) NOT NULL COMMENT '생년월일(문자열 저장)',
    grade           VARCHAR(100) NOT NULL COMMENT '회원 등급(MemberGrade: ROLE_COMMON/ROLE_VIP/ROLE_MANAGE_ADMIN/ROLE_SUPER_ADMIN)',
    social_provider VARCHAR(100) NOT NULL COMMENT '소셜 로그인 제공자(SocialProvider: KAKAO/GOOGLE/NORMAL)',
    created_at      DATETIME(6)  NULL COMMENT '생성 일시',
    updated_at      DATETIME(6)  NULL COMMENT '수정 일시',
    is_delete       BIT(1)       NOT NULL DEFAULT b'0' COMMENT '삭제 여부',
    PRIMARY KEY (member_id),
    KEY idx_member_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '회원';


-- -----------------------------------------------------------------------------
-- movie : 영화
--   base: DefaultEntity
--   soft delete: status='HIDDEN' (is_delete 컬럼 없음)
--     @SQLRestriction("status != 'HIDDEN'")
--     @SQLDelete(... SET status = 'HIDDEN' ...)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movie
(
    movie_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '영화 PK',
    title        VARCHAR(100) NOT NULL COMMENT '제목',
    summary      VARCHAR(255) NOT NULL COMMENT '한줄 요약',
    description  LONGTEXT     NULL COMMENT '상세 설명(@Lob)',
    running_time INT          NOT NULL COMMENT '상영 시간(분)',
    age_rating   VARCHAR(100) NOT NULL COMMENT '관람 등급(MovieAgeRating: ALL/UPPER_12/UPPER_15/ONLY_ADULT)',
    release_date DATE         NOT NULL COMMENT '개봉일',
    close_date   DATE         NOT NULL COMMENT '상영 종료일',
    view_count   BIGINT       NOT NULL DEFAULT 0 COMMENT '조회수',
    status       VARCHAR(100) NOT NULL COMMENT '영화 상태(MovieStatus: DISPLAY/HIDDEN). HIDDEN 이 soft delete',
    category     VARCHAR(100) NOT NULL COMMENT '장르(MovieCategory: ROMANCE/THRILLER/SF)',
    poster_url   VARCHAR(255) NULL COMMENT '포스터 이미지 URL',
    created_at   DATETIME(6)  NULL COMMENT '생성 일시',
    updated_at   DATETIME(6)  NULL COMMENT '수정 일시',
    created_by   BIGINT       NULL COMMENT '생성자',
    updated_by   BIGINT       NULL COMMENT '수정자',
    PRIMARY KEY (movie_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '영화';


-- -----------------------------------------------------------------------------
-- theater : 상영관
--   base: DefaultEntity
--   soft delete: is_delete BIT
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS theater
(
    theater_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '상영관 PK',
    name        VARCHAR(100) NOT NULL COMMENT '상영관 이름',
    description VARCHAR(255) NULL COMMENT '상영관 설명',
    created_at  DATETIME(6)  NULL COMMENT '생성 일시',
    updated_at  DATETIME(6)  NULL COMMENT '수정 일시',
    created_by  BIGINT       NULL COMMENT '생성자',
    updated_by  BIGINT       NULL COMMENT '수정자',
    is_delete   BIT(1)       NOT NULL DEFAULT b'0' COMMENT '삭제 여부',
    PRIMARY KEY (theater_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '상영관';


-- -----------------------------------------------------------------------------
-- theater_seat : 상영관 좌석
--   base: DefaultEntity
--   soft delete: is_delete BIT
--   FK: theater_id -> theater
--   price: 좌석 등급별 가격 (NORMAL 8000 / ADVANCED 10000)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS theater_seat
(
    theater_seat_id BIGINT         NOT NULL AUTO_INCREMENT COMMENT '좌석 PK',
    theater_id      BIGINT         NULL COMMENT '상영관 FK',
    seat_row        VARCHAR(100)   NOT NULL COMMENT '좌석 행(예: A, B)',
    seat_column     VARCHAR(100)   NOT NULL COMMENT '좌석 열(예: 1, 2)',
    grade           VARCHAR(100)   NOT NULL COMMENT '좌석 등급(SeatGrade: NORMAL/ADVANCED)',
    price           DECIMAL(10, 2) NOT NULL COMMENT '좌석 가격(등급별)',
    created_at      DATETIME(6)    NULL COMMENT '생성 일시',
    updated_at      DATETIME(6)    NULL COMMENT '수정 일시',
    created_by      BIGINT         NULL COMMENT '생성자',
    updated_by      BIGINT         NULL COMMENT '수정자',
    is_delete       BIT(1)         NOT NULL DEFAULT b'0' COMMENT '삭제 여부',
    PRIMARY KEY (theater_seat_id),
    KEY idx_theater_seat_theater (theater_id),
    CONSTRAINT fk_theater_seat_theater FOREIGN KEY (theater_id) REFERENCES theater (theater_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '상영관 좌석';


-- -----------------------------------------------------------------------------
-- screening : 상영 회차
--   base: DefaultEntity
--   soft delete: status='DELETE' (is_delete 컬럼 없음)
--     @SQLRestriction("status != 'DELETE'")
--     @SQLDelete(... SET status = 'DELETE' ...)
--   FK: movie_id -> movie, theater_id -> theater
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS screening
(
    screening_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '상영 회차 PK',
    movie_id     BIGINT       NULL COMMENT '영화 FK',
    theater_id   BIGINT       NULL COMMENT '상영관 FK',
    start_date   DATE         NOT NULL COMMENT '상영일',
    start_at     TIME(6)      NOT NULL COMMENT '상영 시작 시각',
    status       VARCHAR(100) NOT NULL COMMENT '상영 상태(ScreeningStatus: PRE_SALE/ON_SALE/END_SALE/SOLD_OUT/DELETE). DELETE 가 soft delete',
    created_at   DATETIME(6)  NULL COMMENT '생성 일시',
    updated_at   DATETIME(6)  NULL COMMENT '수정 일시',
    created_by   BIGINT       NULL COMMENT '생성자',
    updated_by   BIGINT       NULL COMMENT '수정자',
    PRIMARY KEY (screening_id),
    KEY idx_screening_movie (movie_id),
    KEY idx_screening_theater (theater_id),
    CONSTRAINT fk_screening_movie FOREIGN KEY (movie_id) REFERENCES movie (movie_id),
    CONSTRAINT fk_screening_theater FOREIGN KEY (theater_id) REFERENCES theater (theater_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '상영 회차';


-- -----------------------------------------------------------------------------
-- movie_reservation : 영화 예매
--   base: DefaultEntity
--   soft delete 없음 (물리 삭제)
--   FK: member_id -> member, screening_id -> screening,
--       coupon_issuance_id -> coupon_issuance (nullable, 예매당 최대 1개 쿠폰)
--   결제는 1:N — 이 테이블에 movie_payment_id 컬럼은 없고,
--   movie_payment.movie_reservation_id 가 FK 를 가진다.
--   주의: coupon_issuance 는 이 테이블보다 뒤에 정의되지만 파일 상단
--         SET FOREIGN_KEY_CHECKS = 0 덕에 순서와 무관하게 생성된다.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movie_reservation
(
    movie_reservation_id BIGINT         NOT NULL AUTO_INCREMENT COMMENT '예매 PK',
    member_id            BIGINT         NULL COMMENT '예매 회원 FK',
    screening_id         BIGINT         NULL COMMENT '상영 회차 FK',
    coupon_issuance_id   BIGINT         NULL COMMENT '사용 쿠폰 발급 FK (nullable)',
    code                 VARCHAR(255)   NOT NULL COMMENT '예매 번호(고유 코드)',
    seat_names           VARCHAR(255)   NOT NULL COMMENT '예매 좌석명 목록(seat_row+seat_column, 쉼표 구분. 예: A1, B2, C3)',
    total_price          DECIMAL(10, 2) NOT NULL COMMENT '총 결제 금액',
    status               VARCHAR(100)   NOT NULL COMMENT '예매 상태(ReservationStatus: EXPIRED/CANCELED/SUCCESS/PENDING)',
    expired_at           DATETIME(6)    NULL COMMENT '예매 만료 일시(PENDING 홀드 만료)',
    canceled_at          DATETIME(6)    NULL COMMENT '예매 취소 일시',
    created_at           DATETIME(6)    NULL COMMENT '생성 일시',
    updated_at           DATETIME(6)    NULL COMMENT '수정 일시',
    created_by           BIGINT         NULL COMMENT '생성자',
    updated_by           BIGINT         NULL COMMENT '수정자',
    PRIMARY KEY (movie_reservation_id),
    KEY idx_movie_reservation_member (member_id),
    KEY idx_movie_reservation_screening (screening_id),
    KEY idx_movie_reservation_coupon_issuance (coupon_issuance_id),
    CONSTRAINT fk_movie_reservation_member FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT fk_movie_reservation_screening FOREIGN KEY (screening_id) REFERENCES screening (screening_id),
    CONSTRAINT fk_movie_reservation_coupon_issuance FOREIGN KEY (coupon_issuance_id) REFERENCES coupon_issuance (coupon_issuance_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '영화 예매';


-- -----------------------------------------------------------------------------
-- movie_reservation_seat : 예매 좌석(좌석 홀드/점유)
--   base: DefaultEntity
--   soft delete 없음 (물리 삭제)
--   FK: movie_reservation_id -> movie_reservation, screening_id -> screening,
--       theater_seat_id -> theater_seat
--   UNIQUE: (screening_id, theater_seat_id, status)
--     - 동일 상영 회차의 동일 좌석은 점유(status='RESERVED') 상태로 하나만 존재 가능.
--     - 취소/만료 시 애플리케이션에서 status 를 NULL 로 바꿔 제약을 회피한다.
--       (MySQL 은 NULL 을 유니크 제약에서 서로 다른 값으로 취급)
--       -> 그래서 status 는 NOT NULL 이 아니다.
--     - idx_mrs_screening 은 fk_mrs_screening 전용 인덱스다. uk_seat_hold 의
--       선두 컬럼이 screening_id 라 FK 가 그 인덱스에 의존하게 되는데, 그러면
--       uk_seat_hold 를 교체할 때 FK 때문에 드롭이 막히므로 별도로 둔다.
--   좌석 가격은 theater_seat.price 에 있다 (이 테이블에는 없음).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movie_reservation_seat
(
    movie_reservation_seat_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '예매 좌석 PK',
    movie_reservation_id      BIGINT       NULL COMMENT '예매 FK',
    screening_id              BIGINT       NULL COMMENT '상영 회차 FK',
    theater_seat_id           BIGINT       NULL COMMENT '상영관 좌석 FK',
    status                    VARCHAR(100) NULL COMMENT '좌석 점유 상태(ReservationSeatStatus: RESERVED, NULL=취소/만료). 유니크 제약 회피용',
    created_at                DATETIME(6)  NULL COMMENT '생성 일시',
    updated_at                DATETIME(6)  NULL COMMENT '수정 일시',
    created_by                BIGINT       NULL COMMENT '생성자',
    updated_by                BIGINT       NULL COMMENT '수정자',
    PRIMARY KEY (movie_reservation_seat_id),
    UNIQUE KEY uk_seat_hold (screening_id, theater_seat_id, status),
    KEY idx_mrs_reservation (movie_reservation_id),
    KEY idx_mrs_screening (screening_id),
    KEY idx_mrs_theater_seat (theater_seat_id),
    CONSTRAINT fk_mrs_reservation FOREIGN KEY (movie_reservation_id) REFERENCES movie_reservation (movie_reservation_id),
    CONSTRAINT fk_mrs_screening FOREIGN KEY (screening_id) REFERENCES screening (screening_id),
    CONSTRAINT fk_mrs_theater_seat FOREIGN KEY (theater_seat_id) REFERENCES theater_seat (theater_seat_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '예매 좌석(좌석 홀드/점유)';


-- -----------------------------------------------------------------------------
-- movie_payment : 결제 (TODO - 엔티티 골격만 존재)
--   base: DefaultEntity
--   soft delete 없음 (물리 삭제)
--   FK: movie_reservation_id -> movie_reservation (ManyToOne)
--   예매 1 : 결제 N — 부분결제·재결제·환불이 행으로 쌓이므로 유니크 제약을 두지 않는다.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movie_payment
(
    movie_payment_id     BIGINT         NOT NULL AUTO_INCREMENT COMMENT '결제 PK',
    movie_reservation_id BIGINT         NULL COMMENT '예매 FK (ManyToOne)',
    paid_amount          DECIMAL(10, 2) NOT NULL COMMENT '결제 금액',
    status               VARCHAR(100)   NOT NULL COMMENT '결제 상태(PaymentStatus: EXPIRED/CANCELED/SUCCESS/PENDING)',
    method               VARCHAR(100)   NOT NULL COMMENT '결제 수단',
    created_at           DATETIME(6)    NULL COMMENT '생성 일시',
    updated_at           DATETIME(6)    NULL COMMENT '수정 일시',
    created_by           BIGINT         NULL COMMENT '생성자',
    updated_by           BIGINT         NULL COMMENT '수정자',
    PRIMARY KEY (movie_payment_id),
    KEY idx_movie_payment_reservation (movie_reservation_id),
    CONSTRAINT fk_movie_payment_reservation FOREIGN KEY (movie_reservation_id) REFERENCES movie_reservation (movie_reservation_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '결제(TODO)';


-- -----------------------------------------------------------------------------
-- movie_reservation_history : 영화 예매 변경 이력
--   base: HistoryEntity (change_type/change_reason/history_created_at/history_created_by)
--   soft delete 없음, updated_* 없음 (이력은 append-only 성격)
--   예매 변경 시점의 스냅샷을 남긴다. 좌석 목록은 seat_names 문자열로 스냅샷되므로
--   좌석/결제 컬렉션은 따로 두지 않는다 (필요하면 movie_reservation_id 로 원본 조회).
--   스냅샷이므로 원본 생명주기와 독립적이다: 참조 ID 를 값(BIGINT)으로만 보관하고
--   물리적 FK 는 두지 않는다(원본 삭제·변경과 무관하게 이력 보존, 조회용 인덱스만).
--   참조 ID: movie_reservation_id, member_id, screening_id, coupon_issuance_id(nullable)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movie_reservation_history
(
    movie_reservation_history_id BIGINT         NOT NULL AUTO_INCREMENT COMMENT '예매 이력 PK',
    movie_reservation_id         BIGINT         NOT NULL COMMENT '원본 예매 ID(스냅샷, FK 없음)',
    member_id                    BIGINT         NOT NULL COMMENT '예매 회원 ID(스냅샷, FK 없음)',
    screening_id                 BIGINT         NOT NULL COMMENT '상영 회차 ID(스냅샷, FK 없음)',
    coupon_issuance_id           BIGINT         NULL COMMENT '사용 쿠폰 발급 ID(스냅샷, FK 없음)',
    code                         VARCHAR(255)   NOT NULL COMMENT '예매 번호(스냅샷)',
    seat_names                   VARCHAR(255)   NOT NULL COMMENT '예매 좌석명 목록(스냅샷)',
    total_price                  DECIMAL(10, 2) NOT NULL COMMENT '총 결제 금액(스냅샷)',
    expired_at                   DATETIME(6)    NULL COMMENT '예매 만료 일시(스냅샷)',
    canceled_at                  DATETIME(6)    NULL COMMENT '예매 취소 일시(스냅샷)',
    status                       VARCHAR(100)   NOT NULL COMMENT '예매 상태(ReservationStatus)',
    change_type                  VARCHAR(100)   NOT NULL COMMENT '변경 종류(HistoryType: UPDATE)',
    change_reason                VARCHAR(255)   NULL COMMENT '변경 사유',
    history_created_at           DATETIME(6)    NULL COMMENT '이력 생성 일시',
    history_created_by           BIGINT         NULL COMMENT '이력 생성자',
    PRIMARY KEY (movie_reservation_history_id),
    KEY idx_mrh_reservation (movie_reservation_id),
    KEY idx_mrh_member (member_id),
    KEY idx_mrh_screening (screening_id),
    KEY idx_mrh_coupon_issuance (coupon_issuance_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '영화 예매 변경 이력';


-- -----------------------------------------------------------------------------
-- coupon : 쿠폰(발급 대상 정의)
--   base: DefaultEntity
--   soft delete: is_delete BIT
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS coupon
(
    coupon_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '쿠폰 PK',
    name            VARCHAR(100) NOT NULL COMMENT '쿠폰 이름',
    total_quantity  INT          NOT NULL COMMENT '총 발급 가능 수량',
    issued_quantity INT          NOT NULL COMMENT '현재까지 발급된 수량',
    validity_day    INT          NULL COMMENT '발급일 기준 유효 기간(일)',
    start_at        DATETIME(6)  NOT NULL COMMENT '쿠폰 발급 시작 일시',
    end_at          DATETIME(6)  NOT NULL COMMENT '쿠폰 발급 종료 일시',
    created_at      DATETIME(6)  NULL COMMENT '생성 일시',
    updated_at      DATETIME(6)  NULL COMMENT '수정 일시',
    created_by      BIGINT       NULL COMMENT '생성자',
    updated_by      BIGINT       NULL COMMENT '수정자',
    is_delete       BIT(1)       NOT NULL DEFAULT b'0' COMMENT '삭제 여부',
    PRIMARY KEY (coupon_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '쿠폰';


-- -----------------------------------------------------------------------------
-- coupon_issuance : 쿠폰 발급 내역(회원별)
--   base: DefaultEntity
--   FK: member_id -> member, coupon_id -> coupon
--   🔴 is_delete: 엔티티에 isDelete 필드가 없지만
--      @SQLRestriction("is_delete = false") / @SQLDelete 가 raw SQL 로 이 컬럼을
--      참조하므로 반드시 존재해야 한다 (없으면 조회 시 SQL 에러).
--      Hibernate validate 는 raw SQL 을 검사하지 않아 부팅은 통과한다.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS coupon_issuance
(
    coupon_issuance_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '쿠폰 발급 PK',
    member_id          BIGINT       NULL COMMENT '발급 대상 회원 FK',
    coupon_id          BIGINT       NULL COMMENT '쿠폰 FK',
    status             VARCHAR(100) NOT NULL COMMENT '발급 상태(IssuanceStatus: USED/ISSUED/EXPIRED)',
    expired_at         DATETIME(6)  NOT NULL COMMENT '쿠폰 만료 일시',
    used_at            DATETIME(6)  NULL COMMENT '쿠폰 사용 일시',
    created_at         DATETIME(6)  NULL COMMENT '생성 일시',
    updated_at         DATETIME(6)  NULL COMMENT '수정 일시',
    created_by         BIGINT       NULL COMMENT '생성자',
    updated_by         BIGINT       NULL COMMENT '수정자',
    is_delete          BIT(1)       NOT NULL DEFAULT b'0' COMMENT '삭제 여부(엔티티 필드 없음, @SQLRestriction 전용)',
    PRIMARY KEY (coupon_issuance_id),
    KEY idx_coupon_issuance_member (member_id),
    KEY idx_coupon_issuance_coupon (coupon_id),
    CONSTRAINT fk_coupon_issuance_member FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT fk_coupon_issuance_coupon FOREIGN KEY (coupon_id) REFERENCES coupon (coupon_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '쿠폰 발급 내역';


-- -----------------------------------------------------------------------------
-- file_master : 첨부 파일(포스터 등)
--   base: DefaultEntity
--   soft delete: is_delete BIT
--   target_id + target_type(TableType: MOVIE/ITEM) 조합의 polymorphic 참조라
--   물리적 FK 는 둘 수 없다(타입에 따라 참조 테이블이 달라짐) — 조회용 인덱스만 건다.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS file_master
(
    file_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '파일 PK',
    target_id     BIGINT       NOT NULL COMMENT '참조 대상 PK(target_type 에 따라 가리키는 테이블이 달라짐)',
    target_type   VARCHAR(100) NOT NULL COMMENT '참조 대상 테이블(TableType: MOVIE/ITEM)',
    usage_type    VARCHAR(100) NOT NULL COMMENT '파일 용도(UsageType: THUMBNAIL/IMAGE)',
    original_name VARCHAR(255) NULL COMMENT '원본 파일명',
    destination   VARCHAR(255) NULL COMMENT '저장 경로/URL',
    size          BIGINT       NULL COMMENT '파일 크기(byte)',
    content_type  VARCHAR(255) NULL COMMENT 'MIME 타입',
    view_order    INT          NOT NULL DEFAULT 1 COMMENT '노출 순서',
    created_at    DATETIME(6)  NULL COMMENT '생성 일시',
    updated_at    DATETIME(6)  NULL COMMENT '수정 일시',
    created_by    BIGINT       NULL COMMENT '생성자',
    updated_by    BIGINT       NULL COMMENT '수정자',
    is_delete     BIT(1)       NOT NULL DEFAULT b'0' COMMENT '삭제 여부',
    PRIMARY KEY (file_id),
    KEY idx_file_master_target (target_type, target_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '첨부 파일';


-- -----------------------------------------------------------------------------
-- log_master : 변경 이력 로그
--   base: 없음(상속 안 함) — created_at/created_by 만 엔티티에 직접 선언.
--   soft delete 대상 아님(append-only, is_delete 컬럼 없음).
--   주의: 어떤 테이블/레코드의 로그인지 식별하는 컬럼이 엔티티에 없다
--   (body 에 그 정보까지 포함하는 설계로 보임) — schema 도 엔티티 그대로 반영.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS log_master
(
    log_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '로그 PK',
    type       VARCHAR(100) NOT NULL COMMENT '트랜잭션 종류(TransactionType: INSERT/UPDATE/DELETE)',
    body       LONGTEXT     NULL COMMENT '로그 본문(@Lob)',
    created_at DATETIME(6)  NOT NULL COMMENT '생성 일시',
    created_by BIGINT       NOT NULL COMMENT '생성자 회원 PK',
    PRIMARY KEY (log_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '변경 이력 로그';


-- -----------------------------------------------------------------------------
-- common_code_group : 공통 코드 그룹
--   base: DefaultEntity
--   soft delete: is_delete BIT
--   PK 는 문자열 코드(commonCodeGroupId)를 그대로 사용한다.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS common_code_group
(
    common_code_group_id VARCHAR(100) NOT NULL COMMENT '공통 코드 그룹 ID(문자열 코드)',
    group_name           VARCHAR(100) NOT NULL COMMENT '그룹 이름',
    description          VARCHAR(255) NULL COMMENT '그룹 설명',
    created_at           DATETIME(6)  NULL COMMENT '생성 일시',
    updated_at           DATETIME(6)  NULL COMMENT '수정 일시',
    created_by           BIGINT       NULL COMMENT '생성자',
    updated_by           BIGINT       NULL COMMENT '수정자',
    is_delete            BIT(1)       NOT NULL DEFAULT b'0' COMMENT '삭제 여부',
    PRIMARY KEY (common_code_group_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '공통 코드 그룹';


-- -----------------------------------------------------------------------------
-- common_code_detail : 공통 코드 상세
--   base: DefaultEntity
--   soft delete: is_delete BIT
--   복합 PK: (common_code_group_id, common_code_detail_code)  -- @EmbeddedId
--   FK: common_code_group_id -> common_code_group
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS common_code_detail
(
    common_code_group_id    VARCHAR(100) NOT NULL COMMENT '공통 코드 그룹 FK / 복합 PK',
    common_code_detail_code VARCHAR(100) NOT NULL COMMENT '공통 코드 상세 코드 / 복합 PK',
    description             VARCHAR(255) NULL COMMENT '코드 설명',
    sort_order              INT          NOT NULL COMMENT '정렬 순서',
    attr                    VARCHAR(255) NULL COMMENT '부가 속성(자유 값)',
    created_at              DATETIME(6)  NULL COMMENT '생성 일시',
    updated_at              DATETIME(6)  NULL COMMENT '수정 일시',
    created_by              BIGINT       NULL COMMENT '생성자',
    updated_by              BIGINT       NULL COMMENT '수정자',
    is_delete               BIT(1)       NOT NULL DEFAULT b'0' COMMENT '삭제 여부',
    PRIMARY KEY (common_code_group_id, common_code_detail_code),
    CONSTRAINT fk_common_code_detail_group FOREIGN KEY (common_code_group_id) REFERENCES common_code_group (common_code_group_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '공통 코드 상세';


SET FOREIGN_KEY_CHECKS = 1;
