-- =============================================================================
-- cinema-middleware 테스트 데이터 (고정)
-- 대상: MySQL 8.4 / DB: cinema
--
-- 실행할 때마다 15개 테이블을 TRUNCATE 한 뒤 동일 데이터를 재삽입한다 (멱등).
-- movie_reservation_history 는 TRUNCATE 만 하고 데이터는 넣지 않는다
-- (예매 변경 이력은 애플리케이션이 변경 시점에 쌓는 것이라 더미 근거가 없다).
-- 스키마(테이블)는 이 파일에서 만들지 않는다 — 앱 부팅 또는
-- scripts/load-test-data.sh 가 먼저 schema.sql 을 적재한 상태를 전제로 한다.
--
-- ┌───────────────────────────────────────────────────────────────────────────┐
-- │ 기준일: 아래 @today / @now 두 줄만 고치면 모든 상대 날짜가 따라 움직인다.  │
-- │ scripts/load-test-data.sh 는 BASE_DATE 환경변수로 이 두 줄을 치환한다.     │
-- └───────────────────────────────────────────────────────────────────────────┘
--
-- 데이터 규모: member 200 / movie 120 / theater 20 / theater_seat 2000 /
--   screening 500 / movie_reservation 468 / movie_reservation_seat ~1335 /
--   movie_payment ~365 / coupon 100 / coupon_issuance 300 / file_master 120 /
--   common_code_group 15 / common_code_detail 120
--   (log_master 는 테스트 데이터를 넣지 않는다 — 대상 레코드를 식별하는 컬럼이
--   엔티티에 없어 "어떤 로그인지" 의미 있는 더미를 만들 근거가 없다. 테이블만 존재.)
--
-- 비즈니스 특성:
--   - screening: @today 기준 -30 ~ +14일 분산. 과거=END_SALE, 임박=ON_SALE,
--     미래=PRE_SALE, 인기 회차(1~6)=SOLD_OUT.
--   - 예매 1건당 좌석은 최대 4석(컨트롤러 CreateReservationRequest 의
--     @Size(min=1, max=4) 반영). test-data 도 이 규칙을 지킨다.
--   - 예매 상태: 전체 SUCCESS ~78% / CANCELED ~11% / EXPIRED ~7% / PENDING ~4%.
--     (한산 회차만 보면 기존과 동일한 70/15/10/5% — 인기 회차는 "75석 활성 점유"
--      의미를 보존하려 전부 SUCCESS로 고정해서 전체 비율이 SUCCESS 쪽으로 쏠린다.)
--     SUCCESS 예매만 movie_payment 1행.
--   - 좌석 점유: 인기 회차 6개는 각각 80석 중 75석(75%) 점유 — 그 75석을
--     "하나의 예매"가 아니라 19건의 서로 다른 예매(1~4석씩)가 나눠 점유한다
--     (screening 하나를 여러 예매가 공유하는 현실적인 동시 예매 모델링).
--     한산 회차 ~354개는 1~4석 범위로 다양화(1,2,3,4 순환, 회차당 예매 1건) /
--     나머지 빈 회차.
--   - movie_reservation.seat_names: 배정된 좌석의 seat_row+seat_column 을 쉼표로
--     이어붙인 문자열(엔티티 MovieReservation.create() 저장 포맷과 동일, 예: "A1,B2,C3").
--     movie_reservation_seat INSERT(§7) 후 실제 배정 결과를 GROUP_CONCAT 해서
--     UPDATE 로 채운다(§7-1) — 산술을 중복 재현하지 않고 실제 데이터로부터 역산하므로
--     좌석 배정과 seat_names 불일치가 구조적으로 불가능하다.
--   - movie_reservation_seat: SUCCESS/PENDING 은 status='RESERVED',
--     CANCELED/EXPIRED 는 status=NULL (uk_seat_hold 유니크 제약 회피 —
--     NULL 은 서로 다른 값으로 취급). 인기 회차처럼 여러 예매가 screening_id 를
--     공유해도 "screening 내 예매 순번(p) × 4 + 예매 내 좌석 순번(k)" 산술로
--     활성 좌석 슬롯이 예매 간 절대 겹치지 않는다.
--     좌석 가격은 theater_seat.price 로 이관 — 이 테이블에는 price 컬럼 없음.
--   - 가격: theater_seat.price 는 등급별(NORMAL 8000 / ADVANCED 10000, 엔티티
--     TheaterSeat.of() 규칙). screening 에는 price 컬럼이 없다(좌석 가격으로 이관).
--     movie_reservation.total_price 는 배정된 좌석의 theater_seat.price 합계
--     (배정 좌석은 전부 A~H 행 = NORMAL 이라 8000 * 좌석 수).
--   - 쿠폰 사용: SUCCESS 예매 중 약 30% 만 coupon_issuance_id 연결(~109건).
--     연결된 coupon_issuance 는 status='USED'. 각 발급건은 최대 1개 예매에만 연결.
--   - movie.status: close_date 가 @today 이전(상영 종료)이면 'HIDDEN', 아니면 'DISPLAY'.
--     포스터는 movie 테이블에서 제거되고 file_master 로 이관(movie 당 THUMBNAIL 1건,
--     target_type='MOVIE'/target_id=movie_id, 기존 poster_url 과 동일한 URL 포맷 재사용).
--     file_master/log_master 는 target_id+target_type 같은 polymorphic 참조라
--     물리적 FK 가 없다 — FK 고아행 검증 대상에서 제외.
-- =============================================================================

SET NAMES utf8mb4;

-- ★ 기준일 (이 두 줄만 수정) ★
SET @today := DATE '2026-09-03';
SET @now   := TIMESTAMP '2026-09-03 12:00:00';

SET SESSION cte_max_recursion_depth = 100000;   -- 기본 1000 → 상향 (theater_seat 2000행)
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- TRUNCATE (역-의존 순서. FK 체크 꺼진 상태라 순서 무관하나 가독성 위해)
-- -----------------------------------------------------------------------------
TRUNCATE TABLE common_code_detail;
TRUNCATE TABLE common_code_group;
TRUNCATE TABLE log_master;
TRUNCATE TABLE file_master;
TRUNCATE TABLE coupon_issuance;
TRUNCATE TABLE coupon;
TRUNCATE TABLE movie_payment;
TRUNCATE TABLE movie_reservation_history;
TRUNCATE TABLE movie_reservation_seat;
TRUNCATE TABLE movie_reservation;
TRUNCATE TABLE screening;
TRUNCATE TABLE theater_seat;
TRUNCATE TABLE theater;
TRUNCATE TABLE movie;
TRUNCATE TABLE member;


-- =============================================================================
-- 1. member — 200행 (TimeEntity: created_by/updated_by 없음)
--    grade: 1~150 COMMON, 151~190 VIP, 191~198 MANAGE_ADMIN, 199~200 SUPER_ADMIN
-- =============================================================================
INSERT INTO member
  (member_id, email, username, password, phone_number, birthday,
   grade, social_provider, created_at, updated_at, is_delete)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 200)
SELECT
  n,
  CONCAT('member', LPAD(n, 4, '0'), '@example.com'),
  CONCAT('테스트회원', n),
  CONCAT('dummy-password-', LPAD(n, 4, '0')),                 -- 더미(BCrypt 아님, 로그인 불가)
  CONCAT('010', LPAD((n * 13) % 10000, 4, '0'), LPAD((n * 7) % 10000, 4, '0')),
  DATE_FORMAT(@today - INTERVAL (7000 + n * 11) DAY, '%Y%m%d'),  -- birthday 는 VARCHAR
  CASE
    WHEN n <= 150 THEN 'ROLE_COMMON'
    WHEN n <= 190 THEN 'ROLE_VIP'
    WHEN n <= 198 THEN 'ROLE_MANAGE_ADMIN'
    ELSE 'ROLE_SUPER_ADMIN'
  END,
  ELT(1 + (n % 3), 'KAKAO', 'GOOGLE', 'NORMAL'),
  @now, @now, 0
FROM seq;


-- =============================================================================
-- 2. movie — 120행 (DefaultEntity)
--    release_date: @today 기준 -90 ~ +29일, close_date: 항상 release_date 이후
--    status(MovieStatus): close_date 가 @today 이전(상영 종료)이면 'HIDDEN', 아니면 'DISPLAY'.
--      is_active CHAR(1) 이 status 로 대체되었고, HIDDEN 이 soft delete 역할을 겸한다
--      (@SQLRestriction("status != 'HIDDEN'")) — 그래서 is_delete 컬럼이 없다.
--      주의: HIDDEN 인 28행은 JPA 조회에서 자동으로 제외된다.
--    포스터는 더 이상 이 테이블에 없다 — file_master(§13)로 이관.
-- =============================================================================
INSERT INTO movie
  (movie_id, title, summary, description, running_time, age_rating,
   release_date, close_date, view_count, status, category,
   created_at, updated_at, created_by, updated_by)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 120)
SELECT
  n,
  CONCAT('테스트영화 ', LPAD(n, 3, '0')),
  CONCAT(n, '번 영화 한줄 요약'),
  CONCAT(n, '번 영화 상세 설명입니다. 예매 테스트용 더미 데이터.'),
  90 + (n % 8) * 15,                                       -- 90 ~ 195분
  ELT(1 + (n % 4), 'ALL', 'UPPER_12', 'UPPER_15', 'ONLY_ADULT'),
  @today - INTERVAL (90 - (n % 120)) DAY,                  -- -90 ~ +29일
  @today - INTERVAL (90 - (n % 120)) DAY + INTERVAL (35 + (n % 60)) DAY,  -- release + 35~94일
  (n * 137) % 100000,
  CASE WHEN @today - INTERVAL (90 - (n % 120)) DAY + INTERVAL (35 + (n % 60)) DAY < @today
       THEN 'HIDDEN' ELSE 'DISPLAY' END,
  ELT(1 + (n % 3), 'ROMANCE', 'THRILLER', 'SF'),
  @now, @now, NULL, NULL
FROM seq;


-- =============================================================================
-- 3. theater — 20행 (DefaultEntity)
--    좌석 PK 규칙: 관 T 의 좌석 PK = (T-1)*100 + 1 ~ T*100
-- =============================================================================
INSERT INTO theater
  (theater_id, name, description, created_at, updated_at, created_by, updated_by, is_delete)
VALUES
  ( 1, '1관',  '2D 일반 상영관',      @now, @now, NULL, NULL, 0),
  ( 2, '2관',  '2D 일반 상영관',      @now, @now, NULL, NULL, 0),
  ( 3, '3관',  '2D 일반 상영관',      @now, @now, NULL, NULL, 0),
  ( 4, '4관',  '3D 상영관',           @now, @now, NULL, NULL, 0),
  ( 5, '5관',  '3D 상영관',           @now, @now, NULL, NULL, 0),
  ( 6, '6관',  '커플석 특별관',       @now, @now, NULL, NULL, 0),
  ( 7, '7관',  '2D 일반 상영관',      @now, @now, NULL, NULL, 0),
  ( 8, '8관',  '2D 일반 상영관',      @now, @now, NULL, NULL, 0),
  ( 9, '9관',  '리클라이너 상영관',   @now, @now, NULL, NULL, 0),
  (10, '10관', '리클라이너 상영관',   @now, @now, NULL, NULL, 0),
  (11, '11관', '2D 일반 상영관',      @now, @now, NULL, NULL, 0),
  (12, '12관', '2D 일반 상영관',      @now, @now, NULL, NULL, 0),
  (13, '13관', '4DX 상영관',          @now, @now, NULL, NULL, 0),
  (14, '14관', '4DX 상영관',          @now, @now, NULL, NULL, 0),
  (15, '15관', '2D 일반 상영관',      @now, @now, NULL, NULL, 0),
  (16, '16관', '2D 일반 상영관',      @now, @now, NULL, NULL, 0),
  (17, '17관', '돌비 애트모스 상영관', @now, @now, NULL, NULL, 0),
  (18, '18관', '2D 일반 상영관',      @now, @now, NULL, NULL, 0),
  (19, '19관', '프라이빗 소형관',     @now, @now, NULL, NULL, 0),
  (20, '20관', 'IMAX 상영관',         @now, @now, NULL, NULL, 0);


-- =============================================================================
-- 4. theater_seat — 2000행 (DefaultEntity). 20관 × 10행(A~J) × 10열(1~10)
--    좌석 PK = (theater_id-1)*100 + (row-1)*10 + col
--    9~10행(I, J)은 ADVANCED(10000), 나머지 NORMAL(8000) — TheaterSeat.of() 규칙
-- =============================================================================
INSERT INTO theater_seat
  (theater_seat_id, theater_id, seat_row, seat_column, grade, price,
   created_at, updated_at, created_by, updated_by, is_delete)
WITH RECURSIVE
  t(theater_id) AS (SELECT 1 UNION ALL SELECT theater_id + 1 FROM t WHERE theater_id < 20),
  r(ri)         AS (SELECT 1 UNION ALL SELECT ri + 1 FROM r WHERE ri < 10),
  c(ci)         AS (SELECT 1 UNION ALL SELECT ci + 1 FROM c WHERE ci < 10)
SELECT
  (t.theater_id - 1) * 100 + (r.ri - 1) * 10 + c.ci,
  t.theater_id,
  CHAR(64 + r.ri),                                  -- 'A' ~ 'J'
  CAST(c.ci AS CHAR),                               -- '1' ~ '10'
  CASE WHEN r.ri >= 9 THEN 'ADVANCED' ELSE 'NORMAL' END,
  CASE WHEN r.ri >= 9 THEN 10000.00 ELSE 8000.00 END,   -- grade 와 일치
  @now, @now, NULL, NULL, 0
FROM t CROSS JOIN r CROSS JOIN c;


-- =============================================================================
-- 5. screening — 500행 (DefaultEntity)
--    movie_id = 1 + (n % 120), theater_id = 1 + (n % 20)
--    start_date = @today + ((n % 45) - 30)일  → 범위 -30 ~ +14
--    status: n<=6 SOLD_OUT / offset<-1 END_SALE / offset∈[-1,3] ON_SALE / else PRE_SALE
--    soft delete 는 status='DELETE' 로 대체되어 is_delete 컬럼이 없다
--    (@SQLRestriction("status != 'DELETE'")). 테스트 데이터에 DELETE 행은 두지 않는다.
-- =============================================================================
INSERT INTO screening
  (screening_id, movie_id, theater_id, start_date, start_at, status,
   created_at, updated_at, created_by, updated_by)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 500)
SELECT
  n,
  1 + (n % 120),
  1 + (n % 20),
  @today + INTERVAL ((n % 45) - 30) DAY,
  CAST(ELT(1 + (n % 5), '10:00:00', '13:00:00', '16:00:00', '19:00:00', '22:00:00') AS TIME),
  CASE
    WHEN n <= 6                            THEN 'SOLD_OUT'
    WHEN ((n % 45) - 30) < -1              THEN 'END_SALE'
    WHEN ((n % 45) - 30) BETWEEN -1 AND 3  THEN 'ON_SALE'
    ELSE 'PRE_SALE'
  END,
  @now, @now, NULL, NULL
FROM seq;


-- =============================================================================
-- 6. movie_reservation — 468행 (DefaultEntity, soft delete 없음 → is_delete 컬럼 없음)
--    n 1..468 을 두 블록으로 나눈다 (예매 1건당 좌석 최대 4석 규칙 반영):
--      Block A (인기 회차, n=1..114): screening_id = ((n-1) DIV 19) + 1 (1..6),
--        screening 내 예매 순번 p = ((n-1) % 19) + 1 (1..19).
--        seat_count = p<=18 ? 4 : 3 → screening당 합계 18*4+3 = 75석(80석의 75%).
--        status = SUCCESS 고정 (75석을 "활성 점유"로 유지하려면 전부 활성이어야 함).
--      Block B (한산 회차, n=115..468, idle_n = n-114 = 1..354):
--        screening_id = 7 + ((idle_n-1) % 480) — screening 마다 예매 정확히 1건
--        (기존과 동일한 안전장치: screening_id 유일 배정이라 uk_seat_hold 원천 안전).
--        seat_count = 1 + (idle_n % 4) → 1~4 순환(기존 1~2에서 확장).
--        status 분포는 기존 그대로: idle_n%20 기반 SUCCESS70/CANCELED15/EXPIRED10/PENDING5%.
--    (screening 7..486 사용. screening 은 500개라 범위 내.)
--    total_price: 배정 좌석은 전부 A~H 행(NORMAL, 8000) → 8000 * seat_count
--      (seat_count 는 이 INSERT 내부 CTE 계산값일 뿐 테이블 컬럼이 아니다 — 실제
--      예매 좌석 목록은 seat_names 컬럼으로, §7-1 에서 movie_reservation_seat 생성
--      결과를 역산해 채운다. 여기서는 placeholder '' 를 넣어둔다).
--    coupon_issuance_id: SUCCESS 예매에만 순번(sr)을 매겨 sr % 10 < 3 (정확히 30%)
--      일 때 배정. 배정값 = 그 대상들에 다시 매긴 순번 cr (1부터, 최대 ~109).
--      §10 에서 coupon_issuance 1..120 을 status='USED' 로 맞춰 정합.
-- =============================================================================
INSERT INTO movie_reservation
  (movie_reservation_id, member_id, screening_id, coupon_issuance_id, code, seat_names,
   total_price, status, expired_at, canceled_at, created_at, updated_at, created_by, updated_by)
WITH RECURSIVE
  seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 468),
  base AS (
    SELECT
      n,
      CASE WHEN n <= 114 THEN 'POPULAR' ELSE 'IDLE' END AS block,
      ((n - 1) DIV 19) + 1 AS popular_screening_idx,
      ((n - 1) % 19) + 1   AS p_in_screening,
      (n - 114)            AS idle_n
    FROM seq
  ),
  typed AS (
    SELECT
      n,
      1 + (n % 200) AS member_id,
      CASE WHEN block = 'POPULAR' THEN popular_screening_idx
           ELSE 7 + ((idle_n - 1) % 480) END AS screening_id,
      CASE
        WHEN block = 'POPULAR' THEN (CASE WHEN p_in_screening <= 18 THEN 4 ELSE 3 END)
        ELSE 1 + (idle_n % 4)
      END AS seat_count,
      CASE
        WHEN block = 'POPULAR'       THEN 'SUCCESS'
        WHEN (idle_n % 20) < 14      THEN 'SUCCESS'
        WHEN (idle_n % 20) < 17      THEN 'CANCELED'
        WHEN (idle_n % 20) < 19      THEN 'EXPIRED'
        ELSE 'PENDING'
      END AS status
    FROM base
  ),
  ranked AS (
    SELECT
      t.*,
      CASE WHEN t.status = 'SUCCESS'
           THEN ROW_NUMBER() OVER (PARTITION BY t.status ORDER BY t.n)
           ELSE NULL END AS sr
    FROM typed t
  ),
  couponed AS (
    SELECT
      r.*,
      CASE WHEN r.sr IS NOT NULL AND (r.sr % 10) < 3
           THEN ROW_NUMBER() OVER (
                  PARTITION BY (CASE WHEN r.sr IS NOT NULL AND (r.sr % 10) < 3 THEN 0 ELSE 1 END)
                  ORDER BY r.n)
           ELSE NULL END AS coupon_issuance_id
    FROM ranked r
  )
SELECT
  n,
  member_id,
  screening_id,
  coupon_issuance_id,
  CONCAT('R', DATE_FORMAT(@today, '%Y%m%d'), LPAD(n, 6, '0')),
  '',                     -- placeholder, §7-1 에서 실제 배정 좌석으로 UPDATE
  8000 * seat_count,
  status,
  CASE
    WHEN status = 'EXPIRED' THEN @now - INTERVAL ((n % 15) + 1) DAY
    WHEN status = 'PENDING' THEN @now + INTERVAL 10 MINUTE
    ELSE NULL
  END,
  CASE WHEN status = 'CANCELED' THEN @now - INTERVAL ((n % 20) + 1) DAY ELSE NULL END,
  @now, @now, NULL, NULL
FROM couponed;


-- =============================================================================
-- 7. movie_reservation_seat — ~1335행 (DefaultEntity, soft delete 없음 → is_delete 컬럼 없음)
--    좌석 PK 산술:
--      p = ROW_NUMBER() OVER (PARTITION BY screening_id ORDER BY movie_reservation_id)
--        → screening 내에서 이 예매가 몇 번째인지(§6 의 Block A p_in_screening 과 동일
--          하게 재현됨: Block A 는 screening 당 19건이 n 순서로 들어가므로 1..19,
--          Block B 는 screening 이 예매마다 유일하므로 항상 1).
--      seat_cnt: movie_reservation.movie_reservation_id(=§6 의 n) 만으로 §6 typed CTE 의
--        seat_count 산식을 그대로 재현한 값이다 — movie_reservation 테이블에는 좌석 수
--        컬럼이 없으므로(seat_names 문자열만 존재) 여기서 동일 공식을 다시 계산해야 한다.
--        §6 의 CASE 분기와 반드시 일치시킬 것(한쪽만 고치면 좌석 수가 어긋난다).
--      base = (theater_id-1)*100
--      활성 예매(SUCCESS/PENDING): base + (p-1)*4 + k (k=1..seat_cnt<=4), status='RESERVED'
--      비활성 예매(CANCELED/EXPIRED): base + 50 + k, status=NULL
--    인기 회차처럼 여러 예매가 screening_id 를 공유해도 활성 예매는 (p-1)*4 오프셋으로
--    서로 겹치지 않는 슬롯 구간을 배정받는다 (p<=19, k<=4 → 슬롯 최대 75 ⊂ A~H 80석)
--    → (screening, seat, 'RESERVED') 중복 불가 → uk_seat_hold 안전.
--    비활성 좌석은 status=NULL 이라 활성 좌석과 슬롯이 겹쳐도 uk_seat_hold 상 무관하며,
--    이 데이터셋 구조상 같은 screening 에 비활성 예매가 2건 이상 존재하지 않아
--    비활성끼리도 겹치지 않는다.
--    좌석 가격은 theater_seat.price 로 이관됨 — 이 테이블에 price 컬럼 없음.
-- =============================================================================
INSERT INTO movie_reservation_seat
  (movie_reservation_seat_id, movie_reservation_id, screening_id, theater_seat_id,
   status, created_at, updated_at, created_by, updated_by)
WITH RECURSIVE
  ranked_res AS (
    SELECT
      r.*,
      ROW_NUMBER() OVER (PARTITION BY r.screening_id ORDER BY r.movie_reservation_id) AS p,
      CASE
        WHEN r.movie_reservation_id <= 114 THEN
          CASE WHEN ((r.movie_reservation_id - 1) % 19) + 1 <= 18 THEN 4 ELSE 3 END
        ELSE 1 + ((r.movie_reservation_id - 114) % 4)
      END AS seat_cnt
    FROM movie_reservation r
  ),
  k(kk) AS (SELECT 1 UNION ALL SELECT kk + 1 FROM k WHERE kk < 4)
SELECT
  ROW_NUMBER() OVER (ORDER BY r.movie_reservation_id, k.kk),
  r.movie_reservation_id,
  r.screening_id,
  ((s.theater_id - 1) * 100)
    + CASE WHEN r.status IN ('CANCELED', 'EXPIRED') THEN 50 ELSE 0 END
    + CASE WHEN r.status IN ('CANCELED', 'EXPIRED') THEN k.kk
           ELSE (r.p - 1) * 4 + k.kk END,
  CASE WHEN r.status IN ('SUCCESS', 'PENDING') THEN 'RESERVED' ELSE NULL END,
  @now, @now, NULL, NULL
FROM ranked_res r
JOIN screening s ON s.screening_id = r.screening_id
JOIN k ON k.kk <= r.seat_cnt;


-- =============================================================================
-- 7-1. movie_reservation.seat_names 채우기
--    §7 에서 실제로 생성된 movie_reservation_seat(+theater_seat) 결과를 역산해서
--    seat_row+seat_column 을 theater_seat_id 순서로 이어붙인다(산술 재현이 아니라
--    실제 데이터 기반이라 배정 결과와 어긋날 수 없다).
--    포맷은 엔티티 MovieReservation.create() 의 저장 포맷과 동일: 쉼표 구분,
--    공백 없음(예: "A1,B2,C3"). 상태(활성/비활성) 무관하게 이 예매에 연결된
--    모든 좌석을 포함한다 — CANCELED/EXPIRED 라도 "원래 예매했던 좌석"은 남아야 한다.
-- =============================================================================
UPDATE movie_reservation r
JOIN (
  SELECT
    mrs.movie_reservation_id,
    GROUP_CONCAT(CONCAT(ts.seat_row, ts.seat_column) ORDER BY mrs.theater_seat_id SEPARATOR ',') AS seat_names
  FROM movie_reservation_seat mrs
  JOIN theater_seat ts ON ts.theater_seat_id = mrs.theater_seat_id
  GROUP BY mrs.movie_reservation_id
) agg ON agg.movie_reservation_id = r.movie_reservation_id
SET r.seat_names = agg.seat_names;


-- =============================================================================
-- 8. movie_payment — SUCCESS 예매당 1행 (~365행, DefaultEntity)
--    예매 1 : 결제 N 구조(유니크 제약 없음)지만 테스트 데이터는 예매당 1행만 넣는다.
--    soft delete 없음 → is_delete 컬럼 없음.
-- =============================================================================
INSERT INTO movie_payment
  (movie_payment_id, movie_reservation_id, paid_amount, status, method, created_at, updated_at, created_by, updated_by)
SELECT
  ROW_NUMBER() OVER (ORDER BY movie_reservation_id),
  movie_reservation_id,
  total_price,
  'SUCCESS',
  ELT(1 + (movie_reservation_id % 3), 'CARD', 'KAKAO_PAY', 'NAVER_PAY'),
  @now, @now, NULL, NULL
FROM movie_reservation
WHERE status = 'SUCCESS';


-- =============================================================================
-- 9. coupon — 100행 (DefaultEntity). issued_quantity <= total_quantity
-- =============================================================================
INSERT INTO coupon
  (coupon_id, name, total_quantity, issued_quantity, validity_day, start_at, end_at,
   created_at, updated_at, created_by, updated_by, is_delete)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 100)
SELECT
  n,
  CONCAT('테스트쿠폰 ', LPAD(n, 3, '0')),
  100 + (n % 10) * 50,                                       -- 100 ~ 550
  (n % 10) * 20,                                             -- 0 ~ 180 (<= total)
  CASE WHEN n % 3 = 0 THEN NULL ELSE 7 + (n % 4) * 7 END,    -- 일부 NULL, 7~28일
  @now - INTERVAL (n % 30) DAY,
  @now + INTERVAL (30 + (n % 30)) DAY,
  @now, @now, NULL, NULL, 0
FROM seq;


-- =============================================================================
-- 10. coupon_issuance — 300행 (DefaultEntity)
--     status 는 문자열(@Enumerated(STRING) 추가됨): USED / ISSUED / EXPIRED
--       이전에는 ORDINAL 정수(USED=0, ISSUED=1, EXPIRED=2)였다.
--     n 1..120 → USED 강제 (§6 에서 movie_reservation.coupon_issuance_id 로 연결, 최대 ~109)
--     n 121..300 → 기존 분포: ISSUED 50% / USED 30% / EXPIRED 20%
--     is_delete: 엔티티에 isDelete 필드는 없지만 @SQLRestriction 이 raw SQL 로
--       이 컬럼을 참조하므로 반드시 채워야 한다.
-- =============================================================================
INSERT INTO coupon_issuance
  (coupon_issuance_id, member_id, coupon_id, status, expired_at, used_at,
   created_at, updated_at, created_by, updated_by, is_delete)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 300)
SELECT
  n,
  1 + (n % 200),
  1 + (n % 100),
  CASE
    WHEN n <= 120     THEN 'USED'      -- 예매 연결분 + 여유
    WHEN (n % 10) < 5 THEN 'ISSUED'
    WHEN (n % 10) < 8 THEN 'USED'
    ELSE 'EXPIRED'
  END,
  CASE
    WHEN n > 120 AND (n % 10) >= 8 THEN @now - INTERVAL ((n % 20) + 1) DAY   -- EXPIRED 는 과거
    ELSE @now + INTERVAL (30 - (n % 25)) DAY
  END,
  CASE
    WHEN n <= 120                       THEN @now - INTERVAL ((n % 15) + 1) DAY   -- USED
    WHEN (n % 10) BETWEEN 5 AND 7       THEN @now - INTERVAL (n % 15) DAY          -- USED
    ELSE NULL
  END,
  @now, @now, NULL, NULL, 0
FROM seq;


-- =============================================================================
-- 11. file_master — 120행 (DefaultEntity). movie 당 포스터(THUMBNAIL) 1건씩.
--    기존에 movie.poster_url 에 있던 URL 포맷을 그대로 이관(엔티티 변경으로
--    movie 에서는 제거되고 file_master 로 옮겨짐).
-- =============================================================================
INSERT INTO file_master
  (file_id, target_id, target_type, usage_type, original_name, destination, size, content_type, view_order,
   created_at, updated_at, created_by, updated_by, is_delete)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 120)
SELECT
  n,
  n,
  'MOVIE',
  'THUMBNAIL',
  CONCAT('poster_', n, '.jpg'),
  CONCAT('https://cdn.example.com/poster/', n, '.jpg'),
  100000 + (n * 731) % 400000,
  'image/jpeg',
  1,
  @now, @now, NULL, NULL, 0
FROM seq;


-- =============================================================================
-- 12. common_code_group — 15행 (DefaultEntity). PK 는 문자열 코드
-- =============================================================================
INSERT INTO common_code_group
  (common_code_group_id, group_name, description,
   created_at, updated_at, created_by, updated_by, is_delete)
VALUES
  ('MEMBER_GRADE',       '회원 등급',        'MemberGrade enum 미러링',       @now, @now, NULL, NULL, 0),
  ('SOCIAL_PROVIDER',    '소셜 로그인 제공자', 'SocialProvider enum 미러링',    @now, @now, NULL, NULL, 0),
  ('MOVIE_CATEGORY',     '영화 장르',        'MovieCategory enum 미러링',     @now, @now, NULL, NULL, 0),
  ('SEAT_GRADE',         '좌석 등급',        'SeatGrade enum 미러링',         @now, @now, NULL, NULL, 0),
  ('SCREENING_STATUS',   '상영 상태',        'ScreeningStatus enum 미러링',   @now, @now, NULL, NULL, 0),
  ('RESERVATION_STATUS', '예매 상태',        'ReservationStatus enum 미러링', @now, @now, NULL, NULL, 0),
  ('ISSUANCE_STATUS',    '쿠폰 발급 상태',    'IssuanceStatus enum 미러링(STRING)', @now, @now, NULL, NULL, 0),
  ('PAYMENT_METHOD',     '결제 수단',        '결제 수단 분류 코드',           @now, @now, NULL, NULL, 0),
  ('PAYMENT_STATUS',     '결제 상태',        '결제 처리 상태 코드',           @now, @now, NULL, NULL, 0),
  ('CANCEL_REASON',      '예매 취소 사유',    '예매 취소 사유 코드',           @now, @now, NULL, NULL, 0),
  ('THEATER_TYPE',       '상영관 타입',      '상영관 유형 코드',             @now, @now, NULL, NULL, 0),
  ('AGE_RATING',         '관람 등급',        '영상물 관람 등급 코드',         @now, @now, NULL, NULL, 0),
  ('MOVIE_GENRE_EXT',    '확장 장르',        '확장 장르 코드',              @now, @now, NULL, NULL, 0),
  ('NOTIFICATION_TYPE',  '알림 유형',        '알림 발송 유형 코드',           @now, @now, NULL, NULL, 0),
  ('DEVICE_TYPE',        '접속 기기 유형',    '접속 기기 분류 코드',           @now, @now, NULL, NULL, 0);


-- =============================================================================
-- 13. common_code_detail — 120행 (DefaultEntity). 15그룹 × 8행. 복합 PK
--     enum 미러 그룹: 실제 enum name + *_RESERVED_k 예비 코드로 8개 채움
--     ISSUANCE_STATUS: attr 에 과거 ORDINAL 값을 참고용으로 기록(현재 저장은 STRING)
-- =============================================================================
INSERT INTO common_code_detail
  (common_code_group_id, common_code_detail_code, description, sort_order, attr,
   created_at, updated_at, created_by, updated_by, is_delete)
VALUES
  -- MEMBER_GRADE
  ('MEMBER_GRADE', 'ROLE_COMMON',       '일반 회원',      1, NULL,       @now, @now, NULL, NULL, 0),
  ('MEMBER_GRADE', 'ROLE_VIP',          'VIP 회원',       2, NULL,       @now, @now, NULL, NULL, 0),
  ('MEMBER_GRADE', 'ROLE_MANAGE_ADMIN', '운영 관리자',    3, NULL,       @now, @now, NULL, NULL, 0),
  ('MEMBER_GRADE', 'ROLE_SUPER_ADMIN',  '최고 관리자',    4, NULL,       @now, @now, NULL, NULL, 0),
  ('MEMBER_GRADE', 'ROLE_RESERVED_1',   '예비 등급 1',    5, 'reserved', @now, @now, NULL, NULL, 0),
  ('MEMBER_GRADE', 'ROLE_RESERVED_2',   '예비 등급 2',    6, 'reserved', @now, @now, NULL, NULL, 0),
  ('MEMBER_GRADE', 'ROLE_RESERVED_3',   '예비 등급 3',    7, 'reserved', @now, @now, NULL, NULL, 0),
  ('MEMBER_GRADE', 'ROLE_RESERVED_4',   '예비 등급 4',    8, 'reserved', @now, @now, NULL, NULL, 0),
  -- SOCIAL_PROVIDER
  ('SOCIAL_PROVIDER', 'KAKAO',        '카카오',      1, NULL,       @now, @now, NULL, NULL, 0),
  ('SOCIAL_PROVIDER', 'GOOGLE',       '구글',        2, NULL,       @now, @now, NULL, NULL, 0),
  ('SOCIAL_PROVIDER', 'NORMAL',       '일반 가입',    3, NULL,       @now, @now, NULL, NULL, 0),
  ('SOCIAL_PROVIDER', 'PROV_RSV_1',   '예비 제공자 1', 4, 'reserved', @now, @now, NULL, NULL, 0),
  ('SOCIAL_PROVIDER', 'PROV_RSV_2',   '예비 제공자 2', 5, 'reserved', @now, @now, NULL, NULL, 0),
  ('SOCIAL_PROVIDER', 'PROV_RSV_3',   '예비 제공자 3', 6, 'reserved', @now, @now, NULL, NULL, 0),
  ('SOCIAL_PROVIDER', 'PROV_RSV_4',   '예비 제공자 4', 7, 'reserved', @now, @now, NULL, NULL, 0),
  ('SOCIAL_PROVIDER', 'PROV_RSV_5',   '예비 제공자 5', 8, 'reserved', @now, @now, NULL, NULL, 0),
  -- MOVIE_CATEGORY
  ('MOVIE_CATEGORY', 'ROMANCE',    '로맨스',       1, NULL,       @now, @now, NULL, NULL, 0),
  ('MOVIE_CATEGORY', 'THRILLER',   '스릴러',       2, NULL,       @now, @now, NULL, NULL, 0),
  ('MOVIE_CATEGORY', 'SF',         'SF',          3, NULL,       @now, @now, NULL, NULL, 0),
  ('MOVIE_CATEGORY', 'CAT_RSV_1',  '예비 장르 1',  4, 'reserved', @now, @now, NULL, NULL, 0),
  ('MOVIE_CATEGORY', 'CAT_RSV_2',  '예비 장르 2',  5, 'reserved', @now, @now, NULL, NULL, 0),
  ('MOVIE_CATEGORY', 'CAT_RSV_3',  '예비 장르 3',  6, 'reserved', @now, @now, NULL, NULL, 0),
  ('MOVIE_CATEGORY', 'CAT_RSV_4',  '예비 장르 4',  7, 'reserved', @now, @now, NULL, NULL, 0),
  ('MOVIE_CATEGORY', 'CAT_RSV_5',  '예비 장르 5',  8, 'reserved', @now, @now, NULL, NULL, 0),
  -- SEAT_GRADE
  ('SEAT_GRADE', 'NORMAL',      '일반석',       1, '0',        @now, @now, NULL, NULL, 0),
  ('SEAT_GRADE', 'ADVANCED',    '프리미엄석',    2, '3000',     @now, @now, NULL, NULL, 0),
  ('SEAT_GRADE', 'SEAT_RSV_1',  '예비 등급 1',   3, 'reserved', @now, @now, NULL, NULL, 0),
  ('SEAT_GRADE', 'SEAT_RSV_2',  '예비 등급 2',   4, 'reserved', @now, @now, NULL, NULL, 0),
  ('SEAT_GRADE', 'SEAT_RSV_3',  '예비 등급 3',   5, 'reserved', @now, @now, NULL, NULL, 0),
  ('SEAT_GRADE', 'SEAT_RSV_4',  '예비 등급 4',   6, 'reserved', @now, @now, NULL, NULL, 0),
  ('SEAT_GRADE', 'SEAT_RSV_5',  '예비 등급 5',   7, 'reserved', @now, @now, NULL, NULL, 0),
  ('SEAT_GRADE', 'SEAT_RSV_6',  '예비 등급 6',   8, 'reserved', @now, @now, NULL, NULL, 0),
  -- SCREENING_STATUS
  ('SCREENING_STATUS', 'PRE_SALE',   '예매 오픈 전',  1, NULL,       @now, @now, NULL, NULL, 0),
  ('SCREENING_STATUS', 'ON_SALE',    '예매 중',       2, NULL,       @now, @now, NULL, NULL, 0),
  ('SCREENING_STATUS', 'END_SALE',   '예매 종료',     3, NULL,       @now, @now, NULL, NULL, 0),
  ('SCREENING_STATUS', 'SOLD_OUT',   '매진',          4, NULL,       @now, @now, NULL, NULL, 0),
  ('SCREENING_STATUS', 'SCR_RSV_1',  '예비 상태 1',   5, 'reserved', @now, @now, NULL, NULL, 0),
  ('SCREENING_STATUS', 'SCR_RSV_2',  '예비 상태 2',   6, 'reserved', @now, @now, NULL, NULL, 0),
  ('SCREENING_STATUS', 'SCR_RSV_3',  '예비 상태 3',   7, 'reserved', @now, @now, NULL, NULL, 0),
  ('SCREENING_STATUS', 'SCR_RSV_4',  '예비 상태 4',   8, 'reserved', @now, @now, NULL, NULL, 0),
  -- RESERVATION_STATUS
  ('RESERVATION_STATUS', 'PENDING',    '결제 대기',    1, NULL,       @now, @now, NULL, NULL, 0),
  ('RESERVATION_STATUS', 'SUCCESS',    '예매 완료',    2, NULL,       @now, @now, NULL, NULL, 0),
  ('RESERVATION_STATUS', 'CANCELED',   '예매 취소',    3, NULL,       @now, @now, NULL, NULL, 0),
  ('RESERVATION_STATUS', 'EXPIRED',    '홀드 만료',    4, NULL,       @now, @now, NULL, NULL, 0),
  ('RESERVATION_STATUS', 'RES_RSV_1',  '예비 상태 1',  5, 'reserved', @now, @now, NULL, NULL, 0),
  ('RESERVATION_STATUS', 'RES_RSV_2',  '예비 상태 2',  6, 'reserved', @now, @now, NULL, NULL, 0),
  ('RESERVATION_STATUS', 'RES_RSV_3',  '예비 상태 3',  7, 'reserved', @now, @now, NULL, NULL, 0),
  ('RESERVATION_STATUS', 'RES_RSV_4',  '예비 상태 4',  8, 'reserved', @now, @now, NULL, NULL, 0),
  -- ISSUANCE_STATUS (attr 에 과거 ORDINAL 값 참고용)
  ('ISSUANCE_STATUS', 'USED',       '사용 완료',    1, '0',        @now, @now, NULL, NULL, 0),
  ('ISSUANCE_STATUS', 'ISSUED',     '발급됨',       2, '1',        @now, @now, NULL, NULL, 0),
  ('ISSUANCE_STATUS', 'EXPIRED',    '만료됨',       3, '2',        @now, @now, NULL, NULL, 0),
  ('ISSUANCE_STATUS', 'ISS_RSV_1',  '예비 상태 1',  4, 'reserved', @now, @now, NULL, NULL, 0),
  ('ISSUANCE_STATUS', 'ISS_RSV_2',  '예비 상태 2',  5, 'reserved', @now, @now, NULL, NULL, 0),
  ('ISSUANCE_STATUS', 'ISS_RSV_3',  '예비 상태 3',  6, 'reserved', @now, @now, NULL, NULL, 0),
  ('ISSUANCE_STATUS', 'ISS_RSV_4',  '예비 상태 4',  7, 'reserved', @now, @now, NULL, NULL, 0),
  ('ISSUANCE_STATUS', 'ISS_RSV_5',  '예비 상태 5',  8, 'reserved', @now, @now, NULL, NULL, 0),
  -- PAYMENT_METHOD
  ('PAYMENT_METHOD', 'CARD',          '신용/체크카드',  1, NULL, @now, @now, NULL, NULL, 0),
  ('PAYMENT_METHOD', 'KAKAO_PAY',     '카카오페이',     2, NULL, @now, @now, NULL, NULL, 0),
  ('PAYMENT_METHOD', 'NAVER_PAY',     '네이버페이',     3, NULL, @now, @now, NULL, NULL, 0),
  ('PAYMENT_METHOD', 'TOSS',          '토스',          4, NULL, @now, @now, NULL, NULL, 0),
  ('PAYMENT_METHOD', 'BANK_TRANSFER', '계좌이체',       5, NULL, @now, @now, NULL, NULL, 0),
  ('PAYMENT_METHOD', 'POINT',         '포인트',        6, NULL, @now, @now, NULL, NULL, 0),
  ('PAYMENT_METHOD', 'GIFT_CARD',     '기프트카드',     7, NULL, @now, @now, NULL, NULL, 0),
  ('PAYMENT_METHOD', 'MOBILE',        '휴대폰 결제',    8, NULL, @now, @now, NULL, NULL, 0),
  -- PAYMENT_STATUS
  ('PAYMENT_STATUS', 'READY',      '결제 준비',    1, NULL,       @now, @now, NULL, NULL, 0),
  ('PAYMENT_STATUS', 'PAID',       '결제 완료',    2, NULL,       @now, @now, NULL, NULL, 0),
  ('PAYMENT_STATUS', 'CANCELED',   '결제 취소',    3, NULL,       @now, @now, NULL, NULL, 0),
  ('PAYMENT_STATUS', 'PARTIAL_REFUND', '부분 환불', 4, NULL,      @now, @now, NULL, NULL, 0),
  ('PAYMENT_STATUS', 'FAILED',     '결제 실패',    5, NULL,       @now, @now, NULL, NULL, 0),
  ('PAYMENT_STATUS', 'PAY_RSV_1',  '예비 상태 1',  6, 'reserved', @now, @now, NULL, NULL, 0),
  ('PAYMENT_STATUS', 'PAY_RSV_2',  '예비 상태 2',  7, 'reserved', @now, @now, NULL, NULL, 0),
  ('PAYMENT_STATUS', 'PAY_RSV_3',  '예비 상태 3',  8, 'reserved', @now, @now, NULL, NULL, 0),
  -- CANCEL_REASON
  ('CANCEL_REASON', 'CHANGE_MIND',    '단순 변심',      1, NULL,       @now, @now, NULL, NULL, 0),
  ('CANCEL_REASON', 'SCHEDULE',       '일정 변경',      2, NULL,       @now, @now, NULL, NULL, 0),
  ('CANCEL_REASON', 'WRONG_BOOKING',  '잘못 예매',      3, NULL,       @now, @now, NULL, NULL, 0),
  ('CANCEL_REASON', 'PAYMENT_ISSUE',  '결제 문제',      4, NULL,       @now, @now, NULL, NULL, 0),
  ('CANCEL_REASON', 'SYSTEM',         '시스템 취소',    5, NULL,       @now, @now, NULL, NULL, 0),
  ('CANCEL_REASON', 'HOLD_TIMEOUT',   '홀드 시간 초과',  6, NULL,       @now, @now, NULL, NULL, 0),
  ('CANCEL_REASON', 'CANC_RSV_1',     '예비 사유 1',    7, 'reserved', @now, @now, NULL, NULL, 0),
  ('CANCEL_REASON', 'CANC_RSV_2',     '예비 사유 2',    8, 'reserved', @now, @now, NULL, NULL, 0),
  -- THEATER_TYPE
  ('THEATER_TYPE', 'TWO_D',       '2D 일반',        1, NULL,       @now, @now, NULL, NULL, 0),
  ('THEATER_TYPE', 'THREE_D',     '3D',            2, NULL,       @now, @now, NULL, NULL, 0),
  ('THEATER_TYPE', 'IMAX',        'IMAX',          3, NULL,       @now, @now, NULL, NULL, 0),
  ('THEATER_TYPE', 'FOUR_DX',     '4DX',           4, NULL,       @now, @now, NULL, NULL, 0),
  ('THEATER_TYPE', 'RECLINER',    '리클라이너',     5, NULL,       @now, @now, NULL, NULL, 0),
  ('THEATER_TYPE', 'DOLBY',       '돌비 애트모스',   6, NULL,       @now, @now, NULL, NULL, 0),
  ('THEATER_TYPE', 'PRIVATE',     '프라이빗',       7, NULL,       @now, @now, NULL, NULL, 0),
  ('THEATER_TYPE', 'COUPLE',      '커플관',         8, NULL,       @now, @now, NULL, NULL, 0),
  -- AGE_RATING
  ('AGE_RATING', 'ALL',        '전체 관람가',     1, '0',        @now, @now, NULL, NULL, 0),
  ('AGE_RATING', 'AGE_12',     '12세 이상 관람가', 2, '12',       @now, @now, NULL, NULL, 0),
  ('AGE_RATING', 'AGE_15',     '15세 이상 관람가', 3, '15',       @now, @now, NULL, NULL, 0),
  ('AGE_RATING', 'AGE_19',     '청소년 관람불가',  4, '19',       @now, @now, NULL, NULL, 0),
  ('AGE_RATING', 'RESTRICTED', '제한상영가',      5, NULL,       @now, @now, NULL, NULL, 0),
  ('AGE_RATING', 'AGE_RSV_1',  '예비 등급 1',     6, 'reserved', @now, @now, NULL, NULL, 0),
  ('AGE_RATING', 'AGE_RSV_2',  '예비 등급 2',     7, 'reserved', @now, @now, NULL, NULL, 0),
  ('AGE_RATING', 'AGE_RSV_3',  '예비 등급 3',     8, 'reserved', @now, @now, NULL, NULL, 0),
  -- MOVIE_GENRE_EXT
  ('MOVIE_GENRE_EXT', 'ACTION',     '액션',        1, NULL, @now, @now, NULL, NULL, 0),
  ('MOVIE_GENRE_EXT', 'COMEDY',     '코미디',      2, NULL, @now, @now, NULL, NULL, 0),
  ('MOVIE_GENRE_EXT', 'DRAMA',      '드라마',      3, NULL, @now, @now, NULL, NULL, 0),
  ('MOVIE_GENRE_EXT', 'HORROR',     '공포',        4, NULL, @now, @now, NULL, NULL, 0),
  ('MOVIE_GENRE_EXT', 'ANIMATION',  '애니메이션',   5, NULL, @now, @now, NULL, NULL, 0),
  ('MOVIE_GENRE_EXT', 'DOCUMENTARY', '다큐멘터리',  6, NULL, @now, @now, NULL, NULL, 0),
  ('MOVIE_GENRE_EXT', 'FANTASY',    '판타지',      7, NULL, @now, @now, NULL, NULL, 0),
  ('MOVIE_GENRE_EXT', 'MYSTERY',    '미스터리',     8, NULL, @now, @now, NULL, NULL, 0),
  -- NOTIFICATION_TYPE
  ('NOTIFICATION_TYPE', 'RESERVATION_DONE', '예매 완료 알림',  1, NULL, @now, @now, NULL, NULL, 0),
  ('NOTIFICATION_TYPE', 'PAYMENT_DONE',     '결제 완료 알림',  2, NULL, @now, @now, NULL, NULL, 0),
  ('NOTIFICATION_TYPE', 'CANCEL_DONE',      '취소 완료 알림',  3, NULL, @now, @now, NULL, NULL, 0),
  ('NOTIFICATION_TYPE', 'SCREENING_SOON',   '상영 임박 알림',  4, NULL, @now, @now, NULL, NULL, 0),
  ('NOTIFICATION_TYPE', 'COUPON_ISSUED',    '쿠폰 발급 알림',  5, NULL, @now, @now, NULL, NULL, 0),
  ('NOTIFICATION_TYPE', 'COUPON_EXPIRING',  '쿠폰 만료 예정',  6, NULL, @now, @now, NULL, NULL, 0),
  ('NOTIFICATION_TYPE', 'MARKETING',        '마케팅 알림',    7, NULL, @now, @now, NULL, NULL, 0),
  ('NOTIFICATION_TYPE', 'NOTICE',           '공지 알림',      8, NULL, @now, @now, NULL, NULL, 0),
  -- DEVICE_TYPE
  ('DEVICE_TYPE', 'WEB_PC',      'PC 웹',        1, NULL,       @now, @now, NULL, NULL, 0),
  ('DEVICE_TYPE', 'WEB_MOBILE',  '모바일 웹',     2, NULL,       @now, @now, NULL, NULL, 0),
  ('DEVICE_TYPE', 'APP_IOS',     'iOS 앱',       3, NULL,       @now, @now, NULL, NULL, 0),
  ('DEVICE_TYPE', 'APP_ANDROID', 'Android 앱',   4, NULL,       @now, @now, NULL, NULL, 0),
  ('DEVICE_TYPE', 'KIOSK',       '현장 키오스크',  5, NULL,       @now, @now, NULL, NULL, 0),
  ('DEVICE_TYPE', 'DEV_RSV_1',   '예비 기기 1',   6, 'reserved', @now, @now, NULL, NULL, 0),
  ('DEVICE_TYPE', 'DEV_RSV_2',   '예비 기기 2',   7, 'reserved', @now, @now, NULL, NULL, 0),
  ('DEVICE_TYPE', 'DEV_RSV_3',   '예비 기기 3',   8, 'reserved', @now, @now, NULL, NULL, 0);


SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 검증: 테이블별 행수
-- -----------------------------------------------------------------------------
SELECT 'member'                 AS tbl, COUNT(*) AS cnt FROM member
UNION ALL SELECT 'movie',                 COUNT(*) FROM movie
UNION ALL SELECT 'theater',               COUNT(*) FROM theater
UNION ALL SELECT 'theater_seat',          COUNT(*) FROM theater_seat
UNION ALL SELECT 'screening',             COUNT(*) FROM screening
UNION ALL SELECT 'movie_reservation',     COUNT(*) FROM movie_reservation
UNION ALL SELECT 'movie_reservation_seat',COUNT(*) FROM movie_reservation_seat
UNION ALL SELECT 'movie_payment',         COUNT(*) FROM movie_payment
UNION ALL SELECT 'coupon',                COUNT(*) FROM coupon
UNION ALL SELECT 'coupon_issuance',       COUNT(*) FROM coupon_issuance
UNION ALL SELECT 'file_master',           COUNT(*) FROM file_master
UNION ALL SELECT 'log_master',            COUNT(*) FROM log_master
UNION ALL SELECT 'common_code_group',     COUNT(*) FROM common_code_group
UNION ALL SELECT 'common_code_detail',    COUNT(*) FROM common_code_detail;

-- -----------------------------------------------------------------------------
-- 검증: 쿠폰 연결 / 가격 정합성
-- -----------------------------------------------------------------------------
SELECT
  (SELECT COUNT(*) FROM movie_reservation WHERE coupon_issuance_id IS NOT NULL) AS coupon_used_reservations,
  (SELECT COUNT(*) FROM movie_reservation WHERE status = 'SUCCESS')             AS success_reservations,
  -- 연결된 coupon_issuance 가 전부 USED(0) 인지 (0 이어야 정상)
  (SELECT COUNT(*)
     FROM movie_reservation r JOIN coupon_issuance ci ON ci.coupon_issuance_id = r.coupon_issuance_id
     WHERE r.coupon_issuance_id IS NOT NULL AND ci.status <> 0)                 AS coupon_not_used_violation,
  -- total_price 가 8000 * (실제 좌석 수) 와 어긋나는 행 (0 이어야 정상)
  (SELECT COUNT(*) FROM movie_reservation r
     LEFT JOIN (SELECT movie_reservation_id, COUNT(*) AS actual_cnt
                FROM movie_reservation_seat GROUP BY movie_reservation_id) a
       ON a.movie_reservation_id = r.movie_reservation_id
     WHERE r.total_price <> 8000 * COALESCE(a.actual_cnt, 0))                   AS total_price_mismatch,
  -- theater_seat.price 가 grade 와 어긋나는 행 (0 이어야 정상)
  (SELECT COUNT(*) FROM theater_seat
     WHERE (grade = 'NORMAL' AND price <> 8000.00) OR (grade = 'ADVANCED' AND price <> 10000.00)) AS seat_price_mismatch;

-- -----------------------------------------------------------------------------
-- 검증: file_master 정합성 (polymorphic 참조라 물리적 FK 가 없어 애플리케이션 레벨로 확인)
-- -----------------------------------------------------------------------------
SELECT
  -- target_type='MOVIE' 인데 target_id 가 실제 movie 테이블에 없는 행 (0 이어야 정상)
  (SELECT COUNT(*) FROM file_master f
     LEFT JOIN movie m ON m.movie_id = f.target_id
     WHERE f.target_type = 'MOVIE' AND m.movie_id IS NULL)                       AS file_master_movie_orphan,
  -- movie 1건당 file_master(THUMBNAIL) 가 정확히 1건인지 (0 이어야 정상)
  (SELECT COUNT(*) FROM movie m
     LEFT JOIN (SELECT target_id, COUNT(*) AS cnt FROM file_master
                WHERE target_type = 'MOVIE' AND usage_type = 'THUMBNAIL'
                GROUP BY target_id) f ON f.target_id = m.movie_id
     WHERE COALESCE(f.cnt, 0) <> 1)                                              AS movie_thumbnail_count_mismatch;

-- -----------------------------------------------------------------------------
-- 검증: 예매당 좌석 최대 4개 규칙 / seat_names 정합성 / uk_seat_hold
-- -----------------------------------------------------------------------------
SELECT
  -- seat_names 를 쉼표 개수로 역산한 좌석 수가 1~4 범위를 벗어나는 행
  -- (0 이어야 정상, @Size(min=1,max=4) 반영)
  (SELECT COUNT(*) FROM movie_reservation
     WHERE seat_names = ''
        OR (LENGTH(seat_names) - LENGTH(REPLACE(seat_names, ',', '')) + 1) NOT BETWEEN 1 AND 4
  )                                                                              AS seat_count_out_of_range,
  -- seat_names 로 역산한 좌석 수와 실제 movie_reservation_seat 행수가 어긋나는 예매 (0 이어야 정상)
  (SELECT COUNT(*) FROM movie_reservation r
     LEFT JOIN (SELECT movie_reservation_id, COUNT(*) AS actual_cnt
                FROM movie_reservation_seat GROUP BY movie_reservation_id) a
       ON a.movie_reservation_id = r.movie_reservation_id
     WHERE COALESCE(a.actual_cnt, 0)
           <> (LENGTH(r.seat_names) - LENGTH(REPLACE(r.seat_names, ',', '')) + 1)) AS seat_count_actual_mismatch,
  -- seat_names 가 실제 movie_reservation_seat + theater_seat 조합과 어긋나는 행 (0 이어야 정상)
  (SELECT COUNT(*) FROM movie_reservation r
     JOIN (SELECT mrs.movie_reservation_id,
                  GROUP_CONCAT(CONCAT(ts.seat_row, ts.seat_column) ORDER BY mrs.theater_seat_id SEPARATOR ',') AS expected_names
           FROM movie_reservation_seat mrs
           JOIN theater_seat ts ON ts.theater_seat_id = mrs.theater_seat_id
           GROUP BY mrs.movie_reservation_id) agg
       ON agg.movie_reservation_id = r.movie_reservation_id
     WHERE r.seat_names <> agg.expected_names)                                   AS seat_names_mismatch,
  -- 활성(status='RESERVED') 좌석이 같은 회차·같은 좌석에 중복 배정된 행 (0 이어야 정상)
  (SELECT COUNT(*) FROM (
     SELECT screening_id, theater_seat_id FROM movie_reservation_seat
     WHERE status = 'RESERVED'
     GROUP BY screening_id, theater_seat_id
     HAVING COUNT(*) > 1
   ) dup)                                                                        AS uk_seat_hold_violation;
