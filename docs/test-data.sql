-- =============================================================================
-- cinema-middleware 테스트 데이터 (고정)
-- 대상: MySQL 8.4 / DB: cinema
--
-- 실행할 때마다 17개 테이블을 TRUNCATE 한 뒤 동일 데이터를 재삽입한다 (멱등).
-- 스키마(테이블)는 이 파일에서 만들지 않는다 — 앱 부팅 또는
-- scripts/load-test-data.sh 가 먼저 schema.sql 을 적재한 상태를 전제로 한다.
-- 계산용 보조 테이블(tmp_td_*)을 잠시 만들었다가 마지막에 DROP 한다.
--
-- ┌───────────────────────────────────────────────────────────────────────────┐
-- │ 기준일: 아래 @today / @now 두 줄만 고치면 모든 상대 날짜가 따라 움직인다.  │
-- │ scripts/load-test-data.sh 는 BASE_DATE 환경변수로 이 두 줄을 치환한다.     │
-- └───────────────────────────────────────────────────────────────────────────┘
--
-- ■ 로그인 계정 (전원 비밀번호: test1234!  — BCrypt 해시로 저장)
--     user@example.com   김민준  ROLE_COMMON        (예매 내역 다수 보유)
--     vip@example.com    이서연  ROLE_VIP           (예매 내역 다수 보유)
--     admin@example.com  박지훈  ROLE_MANAGE_ADMIN
--     super@example.com  최수아  ROLE_SUPER_ADMIN
--     member00005@example.com ~ member10000@example.com (일반/VIP 고객, 일부 휴면·잠금·탈퇴)
--
-- ■ 데이터 구성 (행 수는 기준일과 무관하게 고정)
--   member 10,000 / movie 36 / cinema 3 / theater 20 / theater_seat 6,000(관당 300석)
--   screening 1,050 (오늘 -7 ~ +6일 × 운영 15관 × 하루 5회)
--   movie_reservation ~2.2만 / movie_reservation_seat ~5만 / movie_payment ~2.2만
--   coupon 10 / coupon_issuance ~1.6천 / file_master 86 / common_code 14그룹 · 49코드
--   movie_reservation_history / movie_payment_history / log_master 는 비워 둔다
--   (애플리케이션이 변경 시점에 쌓는 이력이라 더미 근거가 없다).
--
-- ■ 비즈니스 규칙 (엔티티 로직과 맞춘 것)
--   - 영화관: 강남점·홍대점 OPEN, 부산 해운대점 CLOSE(상영관 UNAVAILABLE, 회차 없음).
--   - 좌석: 관당 A~O 15행 × 1~20열. M~O 3행은 ADVANCED(10,000원), 나머지 NORMAL(8,000원)
--     — SeatGrade enum 가격과 동일. 좌석 PK = (theater_id-1)*300 + (행-1)*20 + 열.
--   - 상영 타입: 4DX관(4, 12) = V_4D, SCREENX관(5, 15) = V_SCREENX, 나머지 V_2D.
--   - 회차 상태: 시작 시각 <= @now → END_SALE / 오늘~+3일 → ON_SALE / +4일~ → PRE_SALE.
--     매진 회차 3개(SOLD_OUT)는 300석 전부 SUCCESS 예매로 채운다.
--   - 회차의 영화는 상영 기간(release_date ~ close_date) 안에서만 배정한다.
--     movie.status='HIDDEN'(soft delete)은 상영 기간이 회차 범위 이전에 끝난 종영작뿐이라
--     어떤 회차도 HIDDEN 영화를 참조하지 않는다.
--   - 예매 수: 회차 날짜(과거 > 임박 > 먼 미래) · 시간대(저녁 19시대 최다) · 주말(+30%)에
--     따라 달라진다. PRE_SALE 회차에는 예매가 없다.
--   - 예매 1건당 1~4석(ReservationRequest @Size(max=4)), 같은 예매의 좌석은 같은 행에
--     붙어 있고, 중앙 좌석부터 채워진다(4석 블록 75개를 선호도 순으로 배정).
--   - 예매 상태: 지난 회차 SUCCESS 85 / CANCELED 10 / EXPIRED 5(%),
--     판매 중 회차 SUCCESS 78 / CANCELED 10 / EXPIRED 7 / PENDING 5(%).
--     PENDING 은 @now 직전 몇 분 안에 만든 홀드라 expired_at 이 미래다.
--   - expired_at = created_at + 5분 (MovieReservation.create 와 동일).
--   - seat_names 는 ", " 구분(예: "H9, H10") — MovieReservation.getSeatCount() 와 동일 포맷.
--   - original_amount = 예매 좌석 가격 합계(실제 배정 좌석에서 역산).
--   - 결제(1:N 시도): SUCCESS → COMPLETE 1건(5%는 앞선 FAILED 시도 포함),
--     CANCELED → 결제 후 취소된 CANCELED 1건, EXPIRED → 40%만 FAILED 시도,
--     PENDING → 1/3 만 진행 중인 PENDING 시도. paid_amount = original_amount
--     (completePayment 가 금액 불일치를 거부하므로 쿠폰 할인은 결제 금액에 반영하지 않는다).
--     movie_reservation.movie_payment_id 는 COMPLETE/CANCELED 결제를 가리킨다.
--   - 쿠폰: SUCCESS 예매의 약 8%가 쿠폰 사용. 사용된 발급건은 예매 회원 본인 것이고
--     status='USED', used_at = 결제 시각. 그 외 발급건은 ISSUED / EXPIRED / USED(매점 쿠폰).
--     coupon.issued_quantity 는 실제 발급 건수와 일치한다.
--   - created_by/updated_by: 관리 데이터(영화·상영관·회차·쿠폰 등)는 관리자(3),
--     예매·결제·쿠폰 발급은 해당 회원 id.
-- =============================================================================

SET NAMES utf8mb4;

-- ★ 기준일 (이 두 줄만 수정) ★
SET @today := DATE '2026-11-01';
SET @now   := TIMESTAMP '2026-11-01 12:00:00';

-- 공통 비밀번호 'test1234!' 의 BCrypt 해시
SET @pw := '$2a$10$iFjxWWpOlnM4kNu6fowJxuLi40gVxYv5ziznQDUYG6pDFU2ZXSNve';
SET @admin := 3;   -- 관리 데이터의 created_by/updated_by

SET SESSION cte_max_recursion_depth = 100000;
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
TRUNCATE TABLE movie_payment_history;
TRUNCATE TABLE movie_payment;
TRUNCATE TABLE movie_reservation_history;
TRUNCATE TABLE movie_reservation_seat;
TRUNCATE TABLE movie_reservation;
TRUNCATE TABLE screening;
TRUNCATE TABLE theater_seat;
TRUNCATE TABLE theater;
TRUNCATE TABLE cinema;
TRUNCATE TABLE movie;
TRUNCATE TABLE member;

-- 보조 테이블 (이전 실행이 중간에 실패했을 때의 잔재 정리)
DROP TABLE IF EXISTS tmp_td_movie, tmp_td_scr, tmp_td_cust, tmp_td_block, tmp_td_res, tmp_td_ci;


-- =============================================================================
-- 1. member — 10,000행 (TimeEntity: created_by/updated_by 없음)
--    1~4: 등급별 대표 계정. 5~10000: 고객(10의 배수 VIP), 297·298 운영 관리자.
--    status: 53의 배수 DELETE(탈퇴, soft delete) / 67의 배수 LOCKED / 41의 배수 SLEEP.
--    phone_number '010'+8자리, birthday 'YYYYMMDD' — MemberRequest 검증 형식과 동일.
-- =============================================================================
INSERT INTO member
  (member_id, email, username, password, phone_number, birthday,
   grade, social_provider, status, created_at, updated_at)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 10000)
SELECT
  n,
  CASE n
    WHEN 1 THEN 'user@example.com'
    WHEN 2 THEN 'vip@example.com'
    WHEN 3 THEN 'admin@example.com'
    WHEN 4 THEN 'super@example.com'
    ELSE CONCAT('member', LPAD(n, 5, '0'), '@example.com')
  END,
  CASE n
    WHEN 1 THEN '김민준'
    WHEN 2 THEN '이서연'
    WHEN 3 THEN '박지훈'
    WHEN 4 THEN '최수아'
    ELSE CONCAT(
      ELT(1 + (n % 20), '김', '이', '박', '최', '정', '강', '조', '윤', '장', '임',
                        '한', '오', '서', '신', '권', '황', '안', '송', '류', '홍'),
      ELT(1 + ((n * 7) % 30), '민준', '서연', '도윤', '서윤', '시우', '지우', '하준', '하은', '주원', '지민',
                              '지호', '수아', '준우', '지유', '준서', '채원', '건우', '윤서', '현우', '다은',
                              '우진', '예은', '선우', '소율', '연우', '지아', '유준', '예린', '정우', '수빈'))
  END,
  @pw,
  CONCAT('010', LPAD((n * 7919) % 1000, 3, '0'), LPAD(n, 5, '0')),        -- n 이 뒷 5자리라 중복 없음
  DATE_FORMAT(DATE '1966-01-01' + INTERVAL ((n * 2741) % 16000) DAY, '%Y%m%d'),  -- 1966 ~ 2009년생
  CASE
    WHEN n = 2                THEN 'ROLE_VIP'
    WHEN n IN (3, 297, 298)   THEN 'ROLE_MANAGE_ADMIN'
    WHEN n = 4                THEN 'ROLE_SUPER_ADMIN'
    WHEN n >= 5 AND n % 10 = 0 THEN 'ROLE_VIP'
    ELSE 'ROLE_COMMON'
  END,
  CASE WHEN n <= 4 THEN 'NORMAL'
       ELSE ELT(1 + (n % 5), 'KAKAO', 'KAKAO', 'GOOGLE', 'NORMAL', 'NORMAL') END,
  CASE
    WHEN n <= 4     THEN 'ACTIVE'
    WHEN n % 53 = 0 THEN 'DELETE'
    WHEN n % 67 = 0 THEN 'LOCKED'
    WHEN n % 41 = 0 THEN 'SLEEP'
    ELSE 'ACTIVE'
  END,
  CASE WHEN n <= 4 THEN @now - INTERVAL 400 DAY
       ELSE @now - INTERVAL (30 + (n * 37) % 700) DAY - INTERVAL ((n * 13) % 1440) MINUTE END,
  CASE WHEN n <= 4 THEN @now - INTERVAL 400 DAY
       ELSE @now - INTERVAL (30 + (n * 37) % 700) DAY - INTERVAL ((n * 13) % 1440) MINUTE END
FROM seq;


-- =============================================================================
-- 2. movie — 36행 (DefaultEntity). 실존 작품이 아닌 가상의 영화다.
--    rel_off / close_off: @today 기준 개봉일·종료일 오프셋(일).
--    weight: 회차 편성 가중치(0 = 회차 범위 안에 상영일이 없음, 3 = 흥행작).
--      1~6   종영작(HIDDEN)       — 회차 범위(-7 ~ +6일) 이전에 상영 종료
--      7~28  현재 상영작(DISPLAY) — 11·12·27 은 곧 종영, 25 는 가족 애니메이션
--      29~36 개봉 예정작(DISPLAY) — 29·30·31 은 회차 범위 안에 개봉(사전 예매)
-- =============================================================================
CREATE TABLE tmp_td_movie
(
  movie_id     BIGINT PRIMARY KEY,
  title        VARCHAR(100),
  summary      VARCHAR(255),
  description  TEXT,
  running_time INT,
  age_rating   VARCHAR(30),
  category     VARCHAR(30),
  rel_off      INT,
  close_off    INT,
  weight       INT,
  view_count   BIGINT
) DEFAULT CHARSET = utf8mb4;

INSERT INTO tmp_td_movie VALUES
  -- 종영작
  ( 1, '여름의 끝에서', '마지막 여름방학, 다시 만난 첫사랑', '졸업 후 십 년 만에 고향 바닷가 마을로 돌아온 두 사람이 그해 여름에 끝내 하지 못한 말을 꺼낸다.', 118, 'UPPER_12', 'ROMANCE', -150, -80, 0, 182340),
  ( 2, '심연의 목격자', '유일한 목격자는 기억을 잃었다', '살인 사건의 유일한 목격자가 사고로 기억을 잃고, 형사는 조각난 기억 속에서 범인을 찾아야 한다.', 127, 'UPPER_15', 'THRILLER', -140, -70, 0, 215470),
  ( 3, '궤도 이탈', '고장 난 정거장, 남은 산소는 72시간', '궤도를 벗어난 우주정거장에 고립된 승무원 셋이 지구 귀환을 위한 마지막 계획을 세운다.', 136, 'UPPER_12', 'SF', -130, -55, 0, 301220),
  ( 4, '편지가 머무는 집', '오래된 집에서 발견한 백 통의 편지', '이사 온 집 다락방에서 발견한 편지를 따라 오십 년 전의 사랑 이야기를 되짚어 가는 휴먼 로맨스.', 112, 'ALL', 'ROMANCE', -120, -45, 0, 98410),
  ( 5, '붉은 장부', '장부 한 권에 도시 전체가 걸렸다', '조직의 비밀 장부를 손에 넣은 회계사가 경찰과 조직 양쪽에 쫓기며 살아남기 위한 거래를 시작한다.', 121, 'ONLY_ADULT', 'THRILLER', -110, -35, 0, 167800),
  ( 6, '마지막 신호', '먼 우주에서 온 마지막 메시지', '전파천문대 연구원이 수신한 의문의 신호가 인류에게 남은 시간을 알려주는 카운트다운이었음이 밝혀진다.', 142, 'UPPER_12', 'SF', -95, -20, 0, 254130),
  -- 현재 상영작
  ( 7, '시간을 건너는 우체부', '어제로 배달되는 편지가 있다면', '하루 전으로 편지를 배달하는 우체부가 사고를 막기 위해 같은 하루를 반복하며 벌어지는 이야기.', 131, 'ALL', 'SF', -45, 30, 3, 512880),
  ( 8, '밤의 증언', '모두가 잠든 밤, 단 한 명의 증인', '심야 편의점 아르바이트생이 목격한 사건을 둘러싸고 거짓 증언이 하나씩 드러나는 서스펜스.', 124, 'UPPER_15', 'THRILLER', -38, 20, 3, 438120),
  ( 9, '봄날의 정류장', '매일 같은 버스, 매일 같은 사람', '출근길 버스 정류장에서 매일 마주치는 두 사람이 계절이 바뀌는 동안 조금씩 가까워진다.', 109, 'UPPER_12', 'ROMANCE', -30, 35, 2, 276540),
  (10, '제로 그래비티 시티', '중력이 사라진 도시에서 살아남기', '실험 사고로 중력이 사라진 도시에서 구조대원이 시민들을 대피시키기 위해 고군분투한다.', 148, 'UPPER_12', 'SF', -25, 40, 3, 497310),
  (11, '침묵의 계약', '말하는 순간 계약은 깨진다', '침묵을 대가로 거액을 받은 내부 고발자가 진실을 밝힐 것인지 선택의 기로에 선다.', 116, 'UPPER_15', 'THRILLER', -60, 3, 1, 189020),
  (12, '파도 위의 고백', '서핑 강습에서 시작된 여름 연애', '제주 바다에서 서핑을 배우던 회사원이 무뚝뚝한 강사와 사랑에 빠지는 로맨틱 코미디.', 105, 'UPPER_12', 'ROMANCE', -52, 5, 1, 143660),
  (13, '나비의 궤적', '작은 선택이 바꾼 세 개의 미래', '한 번의 선택으로 갈라진 세 개의 평행세계에서 같은 사람이 서로 다른 삶을 살아간다.', 122, 'ALL', 'SF', -20, 45, 2, 231450),
  (14, '검은 우산', '비 오는 날마다 사라지는 사람들', '장마철마다 반복되는 실종 사건을 쫓는 신참 형사가 검은 우산을 든 남자를 추적한다.', 119, 'ONLY_ADULT', 'THRILLER', -18, 25, 2, 209870),
  (15, '서로의 계절', '사계절, 네 번의 이별과 재회', '봄·여름·가을·겨울마다 엇갈리는 두 사람의 십 년을 네 개의 장으로 그린 멜로.', 113, 'ALL', 'ROMANCE', -16, 40, 2, 187330),
  (16, '안드로메다 원정대', '인류 최초의 은하 간 항해', '200년 항해를 떠난 원정대가 목적지를 앞두고 선내에서 발생한 반란과 마주한다.', 155, 'UPPER_12', 'SF', -14, 50, 3, 466720),
  (17, '사라진 열차', '출발한 열차가 도착하지 않았다', '승객 120명을 태운 야간열차가 터널 안에서 흔적도 없이 사라지고, 유일한 생존자가 입을 연다.', 128, 'UPPER_15', 'THRILLER', -12, 30, 2, 254980),
  (18, '첫눈이 오면 말할게', '첫눈 오는 날 하기로 한 약속', '첫눈이 오면 고백하겠다는 약속을 미뤄 온 오랜 친구 사이의 두 사람에게 마지막 겨울이 찾아온다.', 107, 'UPPER_12', 'ROMANCE', -10, 45, 2, 198410),
  (19, '인공의 숲', '숲 전체가 하나의 인공지능이라면', '사막화된 지구에 만든 인공 숲이 스스로 의식을 갖게 되면서 인간과 공존을 시험한다.', 133, 'UPPER_15', 'SF', -9, 40, 1, 121870),
  (20, '그림자 추적자', '범인은 늘 한 발 앞에 있다', '연쇄 범죄의 패턴을 분석하던 프로파일러가 범인이 자신을 관찰하고 있다는 사실을 깨닫는다.', 132, 'UPPER_15', 'THRILLER', -8, 35, 2, 213560),
  (21, '라디오 로맨스', '새벽 2시, 라디오로 이어진 마음', '심야 라디오 DJ와 매일 사연을 보내는 익명의 청취자가 전파를 통해 서로를 위로한다.', 102, 'ALL', 'ROMANCE', -7, 30, 1, 96320),
  (22, '퀀텀 하트', '양자 컴퓨터가 사랑을 계산했다', '연애 확률을 계산하는 앱을 개발한 공학자가 예측 불가능한 상대를 만나며 벌어지는 SF 로맨스.', 139, 'UPPER_12', 'SF', -5, 45, 2, 175440),
  (23, '19호실의 비밀', '모든 객실이 비어 있던 호텔', '폭설로 고립된 산장 호텔에서 존재하지 않는 19호실을 둘러싼 투숙객들의 비밀이 드러난다.', 111, 'ONLY_ADULT', 'THRILLER', -4, 30, 1, 88760),
  (24, '달빛 서점', '밤에만 문을 여는 작은 서점', '자정에만 문을 여는 골목 서점의 주인과 불면증에 시달리는 손님이 책으로 마음을 나눈다.', 106, 'ALL', 'ROMANCE', -3, 40, 1, 64210),
  (25, '은하수 정비소', '고장 난 로봇들의 작은 수리점', '은하 변두리 정비소에서 버려진 로봇들을 고치는 소녀의 모험을 그린 가족 애니메이션.', 98, 'ALL', 'SF', -35, 15, 1, 158930),
  (26, '데드라인', '마감까지 남은 시간 6시간', '신문 마감 여섯 시간 전, 특종 기사를 막으려는 세력과 기자의 숨 막히는 추격전.', 114, 'UPPER_15', 'THRILLER', -28, 10, 1, 132470),
  (27, '너에게 가는 길', '천 킬로미터를 걸어서 만나러 간다', '연락이 끊긴 연인을 찾아 국토를 종단하는 청년의 로드무비.', 120, 'UPPER_12', 'ROMANCE', -40, 8, 1, 117650),
  (28, '기억 저장소', '지워진 기억을 사고파는 시대', '기억을 거래하는 회사의 직원이 자신의 기억 일부가 누군가에게 팔렸다는 사실을 알게 된다.', 126, 'UPPER_12', 'SF', -22, 28, 1, 141090),
  -- 개봉 예정작
  (29, '얼어붙은 도시', '영하 40도, 도시가 멈췄다', '이상 한파로 고립된 대도시에서 정전의 원인을 추적하던 엔지니어가 거대한 음모와 마주한다.', 135, 'UPPER_15', 'THRILLER', 2, 50, 3, 52310),
  (30, '별을 세는 밤', '천문대에서 보낸 일주일', '폐관을 앞둔 시골 천문대에서 마지막 관측 주간을 함께 보내는 사람들의 이야기.', 111, 'UPPER_12', 'ROMANCE', 4, 45, 2, 21480),
  (31, '타이탄 프로토콜', '토성의 위성에서 온 구조 요청', '토성 위성 타이탄 기지에서 날아온 구조 요청을 받고 출동한 함대가 예상치 못한 존재를 만난다.', 158, 'UPPER_12', 'SF', 6, 60, 3, 47920),
  (32, '거울 속의 남자', '거울에 비친 나는 내가 아니다', '어느 날부터 거울 속 자신이 다르게 움직이기 시작한 남자의 심리 스릴러.', 118, 'ONLY_ADULT', 'THRILLER', 12, 55, 0, 15630),
  (33, '크리스마스 이브의 약속', '일 년에 단 하루만 만나는 연인', '매년 크리스마스 이브에만 만나기로 약속한 두 사람의 열 번째 크리스마스.', 115, 'ALL', 'ROMANCE', 24, 70, 0, 12870),
  (34, '오르빗 2', '돌아온 궤도, 더 커진 위기', '궤도 이탈 사건 5년 후, 새로운 우주정거장에서 다시 시작되는 생존 미션.', 145, 'UPPER_12', 'SF', 30, 90, 0, 33410),
  (35, '무명의 탐정', '이름 없는 탐정의 첫 번째 사건', '사무소 간판도 없는 무명 탐정이 재벌가 실종 사건을 의뢰받으며 벌어지는 추리극.', 123, 'UPPER_15', 'THRILLER', 45, 100, 0, 8420),
  (36, '겨울 정원', '눈 덮인 정원에서 피어난 꽃', '겨울에만 꽃이 피는 정원을 가꾸는 노부부와 그 정원을 찾아온 젊은 사진가의 이야기.', 108, 'ALL', 'ROMANCE', 52, 110, 0, 5170);

INSERT INTO movie
  (movie_id, title, summary, description, running_time, age_rating,
   release_date, close_date, view_count, status, category,
   created_at, updated_at, created_by, updated_by)
SELECT
  movie_id, title, summary, description, running_time, age_rating,
  @today + INTERVAL rel_off DAY,
  @today + INTERVAL close_off DAY,
  view_count,
  CASE WHEN close_off < -7 THEN 'HIDDEN' ELSE 'DISPLAY' END,   -- 회차 범위 이전 종영작만 HIDDEN
  category,
  @now - INTERVAL (GREATEST(30 - rel_off, 30)) DAY,             -- 개봉 30일 전(또는 30일 전) 등록
  @now - INTERVAL (GREATEST(30 - rel_off, 30)) DAY,
  @admin, @admin
FROM tmp_td_movie;


-- =============================================================================
-- 3. cinema — 3행 (DefaultEntity). soft delete 는 status='DELETE'
-- =============================================================================
INSERT INTO cinema
  (cinema_id, name, description, status, created_at, updated_at, created_by, updated_by)
VALUES
  (1, '강남점',     '서울 강남역 11번 출구 도보 3분. 4DX·SCREENX 특별관 보유, 8개 상영관.', 'OPEN',  @now - INTERVAL 900 DAY, @now - INTERVAL 900 DAY, @admin, @admin),
  (2, '홍대점',     '서울 홍대입구역 9번 출구 연결. 4DX·SCREENX 특별관 보유, 7개 상영관.', 'OPEN',  @now - INTERVAL 700 DAY, @now - INTERVAL 700 DAY, @admin, @admin),
  (3, '부산 해운대점', '부산 해운대해수욕장 인근. 리뉴얼 공사로 임시 휴관 중.',          'CLOSE', @now - INTERVAL 500 DAY, @now - INTERVAL 20 DAY,  @admin, @admin);


-- =============================================================================
-- 4. theater — 20행 (DefaultEntity)
--    강남점 1~8 / 홍대점 9~15 / 부산 해운대점 16~20(휴관 → UNAVAILABLE)
--    특별관: 4DX = 4, 12 / SCREENX = 5, 15 (screening.screen_type 이 이를 따른다)
-- =============================================================================
INSERT INTO theater
  (theater_id, cinema_id, name, description, status, created_at, updated_at, created_by, updated_by)
VALUES
  ( 1, 1, '1관', '2D 일반관',                 'AVAILABLE',   @now - INTERVAL 900 DAY, @now - INTERVAL 900 DAY, @admin, @admin),
  ( 2, 1, '2관', '2D 일반관',                 'AVAILABLE',   @now - INTERVAL 900 DAY, @now - INTERVAL 900 DAY, @admin, @admin),
  ( 3, 1, '3관', '2D 일반관',                 'AVAILABLE',   @now - INTERVAL 900 DAY, @now - INTERVAL 900 DAY, @admin, @admin),
  ( 4, 1, '4관', '4DX 특별관 (모션 체어·환경 효과)', 'AVAILABLE', @now - INTERVAL 900 DAY, @now - INTERVAL 900 DAY, @admin, @admin),
  ( 5, 1, '5관', 'SCREENX 특별관 (3면 스크린)',  'AVAILABLE',   @now - INTERVAL 900 DAY, @now - INTERVAL 900 DAY, @admin, @admin),
  ( 6, 1, '6관', '리클라이너관',               'AVAILABLE',   @now - INTERVAL 900 DAY, @now - INTERVAL 900 DAY, @admin, @admin),
  ( 7, 1, '7관', '2D 일반관',                 'AVAILABLE',   @now - INTERVAL 900 DAY, @now - INTERVAL 900 DAY, @admin, @admin),
  ( 8, 1, '8관', '2D 일반관',                 'AVAILABLE',   @now - INTERVAL 900 DAY, @now - INTERVAL 900 DAY, @admin, @admin),
  ( 9, 2, '1관', '2D 일반관',                 'AVAILABLE',   @now - INTERVAL 700 DAY, @now - INTERVAL 700 DAY, @admin, @admin),
  (10, 2, '2관', '2D 일반관',                 'AVAILABLE',   @now - INTERVAL 700 DAY, @now - INTERVAL 700 DAY, @admin, @admin),
  (11, 2, '3관', '2D 일반관',                 'AVAILABLE',   @now - INTERVAL 700 DAY, @now - INTERVAL 700 DAY, @admin, @admin),
  (12, 2, '4관', '4DX 특별관 (모션 체어·환경 효과)', 'AVAILABLE', @now - INTERVAL 700 DAY, @now - INTERVAL 700 DAY, @admin, @admin),
  (13, 2, '5관', '2D 일반관',                 'AVAILABLE',   @now - INTERVAL 700 DAY, @now - INTERVAL 700 DAY, @admin, @admin),
  (14, 2, '6관', '리클라이너관',               'AVAILABLE',   @now - INTERVAL 700 DAY, @now - INTERVAL 700 DAY, @admin, @admin),
  (15, 2, '7관', 'SCREENX 특별관 (3면 스크린)',  'AVAILABLE',   @now - INTERVAL 700 DAY, @now - INTERVAL 700 DAY, @admin, @admin),
  (16, 3, '1관', '2D 일반관 (휴관)',           'UNAVAILABLE', @now - INTERVAL 500 DAY, @now - INTERVAL 20 DAY,  @admin, @admin),
  (17, 3, '2관', '2D 일반관 (휴관)',           'UNAVAILABLE', @now - INTERVAL 500 DAY, @now - INTERVAL 20 DAY,  @admin, @admin),
  (18, 3, '3관', '2D 일반관 (휴관)',           'UNAVAILABLE', @now - INTERVAL 500 DAY, @now - INTERVAL 20 DAY,  @admin, @admin),
  (19, 3, '4관', '리클라이너관 (휴관)',         'UNAVAILABLE', @now - INTERVAL 500 DAY, @now - INTERVAL 20 DAY,  @admin, @admin),
  (20, 3, '5관', '2D 일반관 (휴관)',           'UNAVAILABLE', @now - INTERVAL 500 DAY, @now - INTERVAL 20 DAY,  @admin, @admin);


-- =============================================================================
-- 5. theater_seat — 6,000행 (DefaultEntity). 20관 × 15행(A~O) × 20열(1~20) = 관당 300석
--    좌석 PK = (theater_id-1)*300 + (행-1)*20 + 열
--    M~O(13~15행) ADVANCED 10,000원 / 나머지 NORMAL 8,000원 (SeatGrade enum 가격)
-- =============================================================================
INSERT INTO theater_seat
  (theater_seat_id, theater_id, seat_row, seat_column, grade, price, status,
   created_at, updated_at, created_by, updated_by)
WITH RECURSIVE
  t(theater_id) AS (SELECT 1 UNION ALL SELECT theater_id + 1 FROM t WHERE theater_id < 20),
  r(ri)         AS (SELECT 1 UNION ALL SELECT ri + 1 FROM r WHERE ri < 15),
  c(ci)         AS (SELECT 1 UNION ALL SELECT ci + 1 FROM c WHERE ci < 20)
SELECT
  (t.theater_id - 1) * 300 + (r.ri - 1) * 20 + c.ci,
  t.theater_id,
  CHAR(64 + r.ri USING utf8mb4),                    -- 'A' ~ 'O'
  CAST(c.ci AS CHAR),                               -- '1' ~ '20'
  CASE WHEN r.ri >= 13 THEN 'ADVANCED' ELSE 'NORMAL' END,
  CASE WHEN r.ri >= 13 THEN 10000.00 ELSE 8000.00 END,
  'AVAILABLE',
  @now - INTERVAL 500 DAY, @now - INTERVAL 500 DAY, @admin, @admin
FROM t CROSS JOIN r CROSS JOIN c;


-- =============================================================================
-- 6. screening — 1,050행 (DefaultEntity)
--    운영 중인 15개 관(1~15) × 14일(@today-7 ~ +6) × 하루 5회
--    (09:40 / 12:50 / 16:00 / 19:10 / 22:20 — 최장 러닝타임 158분 < 회차 간격 190분)
--    영화 배정: 그날 상영 기간 안에 있는 영화 목록을 weight 만큼 복제한 뒤
--      (관·회차·날짜) 산술로 하나를 고른다 → 흥행작일수록 많은 회차.
--    매진(SOLD_OUT): (오늘, 홍대 1관, 22:20) / (+1일, 강남 1관, 19:10) / (+2일, 강남 4DX관, 19:10)
-- =============================================================================
CREATE TABLE tmp_td_scr
(
  screening_id BIGINT PRIMARY KEY,
  theater_id   BIGINT,
  movie_id     BIGINT,
  off          INT,
  slot         INT,
  start_date   DATE,
  start_at     TIME,
  start_dt     DATETIME,
  status       VARCHAR(30)
);

INSERT INTO tmp_td_scr
WITH RECURSIVE
  d(off)        AS (SELECT -7 UNION ALL SELECT off + 1 FROM d WHERE off < 6),
  w(i)          AS (SELECT 1 UNION ALL SELECT i + 1 FROM w WHERE i < 3),
  th(theater_id) AS (SELECT 1 UNION ALL SELECT theater_id + 1 FROM th WHERE theater_id < 15),
  sl(slot)      AS (SELECT 1 UNION ALL SELECT slot + 1 FROM sl WHERE slot < 5),
  cand AS (
    SELECT d.off, m.movie_id,
           ROW_NUMBER() OVER (PARTITION BY d.off ORDER BY m.movie_id, w.i) AS rn
    FROM d
    JOIN tmp_td_movie m ON m.rel_off <= d.off AND m.close_off >= d.off AND m.weight > 0
    JOIN w ON w.i <= m.weight
  ),
  cnt AS (SELECT off, COUNT(*) AS c FROM cand GROUP BY off),
  grid AS (
    SELECT
      d.off, th.theater_id, sl.slot,
      1 + MOD(th.theater_id * 7 + sl.slot * 11 + (d.off + 7) * 3, cnt.c) AS pick,
      DATE(@today + INTERVAL d.off DAY) AS start_date,
      CAST(ELT(sl.slot, '09:40:00', '12:50:00', '16:00:00', '19:10:00', '22:20:00') AS TIME) AS start_at
    FROM d CROSS JOIN th CROSS JOIN sl
    JOIN cnt ON cnt.off = d.off
  )
SELECT
  ROW_NUMBER() OVER (ORDER BY g.off, g.theater_id, g.slot),
  g.theater_id,
  c.movie_id,
  g.off,
  g.slot,
  g.start_date,
  g.start_at,
  TIMESTAMP(g.start_date, g.start_at),
  CASE
    WHEN (g.off, g.theater_id, g.slot) IN ((0, 9, 5), (1, 1, 4), (2, 4, 4)) THEN 'SOLD_OUT'
    WHEN TIMESTAMP(g.start_date, g.start_at) <= TIMESTAMP(@now)              THEN 'END_SALE'
    WHEN g.off <= 3                                                          THEN 'ON_SALE'
    ELSE 'PRE_SALE'
  END
FROM grid g
JOIN cand c ON c.off = g.off AND c.rn = g.pick;

INSERT INTO screening
  (screening_id, movie_id, theater_id, screen_type, start_date, start_at, status,
   created_at, updated_at, created_by, updated_by)
SELECT
  screening_id,
  movie_id,
  theater_id,
  CASE WHEN theater_id IN (4, 12) THEN 'V_4D'
       WHEN theater_id IN (5, 15) THEN 'V_SCREENX'
       ELSE 'V_2D' END,
  start_date,
  start_at,
  status,
  -- 회차는 상영일 10일 전에 등록(단, 기준 시각보다 미래가 되지 않게)
  LEAST(TIMESTAMP(start_date - INTERVAL 10 DAY, '10:00:00'), TIMESTAMP(@now) - INTERVAL 1 DAY),
  CASE WHEN status IN ('END_SALE', 'SOLD_OUT') THEN LEAST(start_dt, TIMESTAMP(@now))
       ELSE LEAST(TIMESTAMP(start_date - INTERVAL 10 DAY, '10:00:00'), TIMESTAMP(@now) - INTERVAL 1 DAY) END,
  @admin, @admin
FROM tmp_td_scr;


-- =============================================================================
-- 7. 예매 계산용 보조 테이블
--    tmp_td_cust : 예매 가능한 고객(ACTIVE + ROLE_COMMON/ROLE_VIP) 목록.
--                  rn 은 member_id 와 무관하게 해시로 섞은 순서 — 앞쪽 10% 단골, 60% 최근 활동 고객.
--    tmp_td_block: 4석 블록 75개(15행 × 5블록)의 선호도 순위 — 중앙 행(H~K)·중앙 블록부터
--    tmp_td_res  : 예매 1건당 1행 (상태·좌석 수·시각·결제/쿠폰 여부를 미리 계산)
-- =============================================================================
CREATE TABLE tmp_td_cust
(
  rn        INT PRIMARY KEY,
  member_id BIGINT,
  grade     VARCHAR(30)
);
INSERT INTO tmp_td_cust
SELECT ROW_NUMBER() OVER (ORDER BY MOD(member_id * 7919, 10007), member_id), member_id, grade
FROM member
WHERE status = 'ACTIVE' AND grade IN ('ROLE_COMMON', 'ROLE_VIP');

SET @cust_cnt := (SELECT COUNT(*) FROM tmp_td_cust);

CREATE TABLE tmp_td_block
(
  rnk    INT PRIMARY KEY,
  row_no INT,
  blk    INT
);
INSERT INTO tmp_td_block
WITH RECURSIVE
  r(row_no) AS (SELECT 1 UNION ALL SELECT row_no + 1 FROM r WHERE row_no < 15),
  b(blk)    AS (SELECT 1 UNION ALL SELECT blk + 1 FROM b WHERE blk < 5)
SELECT
  ROW_NUMBER() OVER (ORDER BY ABS(2 * row_no - 19), ABS(blk - 3), row_no DESC, blk),
  row_no, blk
FROM r CROSS JOIN b;

CREATE TABLE tmp_td_res
(
  rid         BIGINT PRIMARY KEY,
  screening_id BIGINT,
  theater_id  BIGINT,
  member_id   BIGINT,
  p           INT,          -- 회차 내 예매 순번 = 배정 블록 순위
  seat_cnt    INT,
  status      VARCHAR(30),
  code        VARCHAR(40),
  created_at  DATETIME(6),
  paid_at     DATETIME(6),
  canceled_at DATETIME(6),
  fail_before TINYINT,      -- 앞선 FAILED 결제 시도 존재
  pending_pay TINYINT,      -- 진행 중(PENDING) 결제 시도 존재
  use_coupon  TINYINT,
  method      VARCHAR(30)
);

-- 회차당 예매 수 = 날짜 기준치 × 시간대 계수 × 주말 계수 × 회차별 편차(70~130%), 최대 75
--   날짜 기준치: 지난 회차 45 / 오늘 38 / +1일 26 / +2일 16 / +3일 9 / PRE_SALE 0
--   시간대 계수(%): 09:40 40 / 12:50 60 / 16:00 70 / 19:10 100 / 22:20 65
INSERT INTO tmp_td_res
  (rid, screening_id, theater_id, member_id, p, seat_cnt, status, code,
   created_at, paid_at, canceled_at, fail_before, pending_pay, use_coupon, method)
WITH RECURSIVE
  seq(p) AS (SELECT 1 UNION ALL SELECT p + 1 FROM seq WHERE p < 75),
  scr AS (
    SELECT
      s.*,
      CASE
        WHEN s.status = 'SOLD_OUT' THEN 75
        WHEN s.status = 'PRE_SALE' THEN 0
        ELSE LEAST(75, FLOOR(
               (CASE WHEN s.status = 'END_SALE' THEN 45
                     WHEN s.off = 0 THEN 38 WHEN s.off = 1 THEN 26 WHEN s.off = 2 THEN 16 ELSE 9 END)
             * (CASE s.slot WHEN 1 THEN 40 WHEN 2 THEN 60 WHEN 3 THEN 70 WHEN 4 THEN 100 ELSE 65 END)
             * (CASE WHEN DAYOFWEEK(s.start_date) IN (1, 7) THEN 130 ELSE 100 END)
             * (70 + MOD(s.screening_id * 37, 61))
             / 1000000))
      END AS res_cnt
    FROM tmp_td_scr s
  ),
  base AS (
    SELECT
      ROW_NUMBER() OVER (ORDER BY scr.screening_id, seq.p) AS rid,
      scr.screening_id, scr.theater_id, scr.status AS scr_status,
      scr.start_date, scr.start_dt, seq.p,
      MOD(scr.screening_id * 7919 + seq.p * 104729, 100) AS h1,
      MOD(scr.screening_id * 131 + seq.p * 977, 100)     AS h2
    FROM scr
    JOIN seq ON seq.p <= scr.res_cnt
  ),
  typed AS (
    SELECT
      b.*,
      CASE WHEN b.scr_status = 'SOLD_OUT' THEN 4
           WHEN b.h1 < 20 THEN 1 WHEN b.h1 < 70 THEN 2 WHEN b.h1 < 85 THEN 3 ELSE 4 END AS seat_cnt,
      CASE
        WHEN b.scr_status = 'SOLD_OUT' THEN 'SUCCESS'
        WHEN b.scr_status = 'END_SALE' THEN
          CASE WHEN b.h2 < 85 THEN 'SUCCESS' WHEN b.h2 < 95 THEN 'CANCELED' ELSE 'EXPIRED' END
        ELSE
          CASE WHEN b.h2 < 78 THEN 'SUCCESS' WHEN b.h2 < 88 THEN 'CANCELED'
               WHEN b.h2 < 95 THEN 'EXPIRED' ELSE 'PENDING' END
      END AS status,
      -- 예매 시각 기준점: 지난 회차는 시작 20분 전, 판매 중 회차는 기준 시각
      CASE WHEN b.start_dt <= TIMESTAMP(@now) THEN b.start_dt - INTERVAL 20 MINUTE
           ELSE TIMESTAMP(@now) END AS base_at
    FROM base b
  ),
  timed AS (
    SELECT
      t.*,
      CASE WHEN t.status = 'PENDING' THEN TIMESTAMP(@now) - INTERVAL MOD(t.rid, 4) MINUTE   -- 홀드 중
           ELSE t.base_at - INTERVAL (10 + MOD(t.rid * 7919, 4300)) MINUTE END AS created_at  -- 최대 약 3일 전
    FROM typed t
  )
SELECT
  t.rid,
  t.screening_id,
  t.theater_id,
  CASE WHEN MOD(t.rid, 1500) = 0 THEN 1        -- 대표 계정(user@) 예매 내역 ~15건
       WHEN MOD(t.rid, 1700) = 0 THEN 2        -- 대표 계정(vip@) ~13건
       ELSE c.member_id END,
  t.p,
  t.seat_cnt,
  t.status,
  CONCAT('R', DATE_FORMAT(t.start_date, '%y%m%d'), '-', LPAD(t.rid, 6, '0')),
  t.created_at,
  CASE WHEN t.status IN ('SUCCESS', 'CANCELED')
       THEN t.created_at + INTERVAL (60 + MOD(t.rid, 180)) SECOND END,                  -- 1~4분 뒤 결제
  CASE WHEN t.status = 'CANCELED'
       THEN LEAST(t.created_at + INTERVAL (60 + MOD(t.rid, 180)) SECOND + INTERVAL (10 + MOD(t.rid * 31, 2000)) MINUTE,
                  CASE WHEN t.start_dt <= TIMESTAMP(@now) THEN t.start_dt - INTERVAL 1 MINUTE
                       ELSE TIMESTAMP(@now) - INTERVAL 1 MINUTE END) END,               -- 상영 전·기준 시각 전 취소
  CASE WHEN (t.status = 'SUCCESS' AND MOD(t.rid, 20) = 0)
         OR (t.status = 'EXPIRED' AND MOD(t.rid, 5) < 2) THEN 1 ELSE 0 END,
  CASE WHEN t.status = 'PENDING' AND MOD(t.rid, 3) = 0 THEN 1 ELSE 0 END,
  CASE WHEN t.status = 'SUCCESS' AND MOD(t.rid * 13, 100) < 8 THEN 1 ELSE 0 END,
  ELT(1 + MOD(t.rid, 3), 'CARD', 'KAKAO', 'TOSS')
FROM timed t
-- 예매 회원 (CRC32 해시로 무작위에 가깝게 선택 → 회원별 예매 수가 포아송처럼 퍼진다)
--   30%: 단골(섞인 순서 상위 10%) / 70%: 최근 활동 고객(상위 60%, 단골 포함)
--   → 나머지 40% 고객은 이 2주간 예매가 없다.
JOIN tmp_td_cust c ON c.rn = CASE
  WHEN MOD(t.rid * 31, 10) < 3 THEN 1 + MOD(CRC32(CONCAT('heavy-', t.rid)), FLOOR(@cust_cnt / 10))
  ELSE 1 + MOD(CRC32(CONCAT('casual-', t.rid)), FLOOR(@cust_cnt * 6 / 10))
END;


-- =============================================================================
-- 8. movie_reservation (DefaultEntity, 물리 삭제)
--    seat_names / original_amount 는 좌석 INSERT(§9) 후 실제 배정 좌석에서 역산한다(§9-1).
--    coupon_issuance_id(§12), movie_payment_id(§10-1) 도 뒤에서 UPDATE 로 연결한다.
-- =============================================================================
INSERT INTO movie_reservation
  (movie_reservation_id, member_id, screening_id, coupon_issuance_id, movie_payment_id,
   code, seat_names, original_amount, status, expired_at,
   created_at, updated_at, created_by, updated_by)
SELECT
  rid, member_id, screening_id, NULL, NULL,
  code, '', 0, status,
  created_at + INTERVAL 5 MINUTE,
  created_at,
  CASE status
    WHEN 'SUCCESS'  THEN paid_at
    WHEN 'CANCELED' THEN canceled_at
    WHEN 'EXPIRED'  THEN created_at + INTERVAL 5 MINUTE
    ELSE created_at
  END,
  member_id, member_id
FROM tmp_td_res;


-- =============================================================================
-- 9. movie_reservation_seat (DefaultEntity, 물리 삭제)
--    예매 순번 p → 선호도 p 위 블록. 좌석은 블록 안에서 연속 배정
--    (1~2석은 블록 가운데 2·3번째 칸부터). 회차마다 예매가 서로 다른 블록을 쓰므로
--    (screening, seat, 'RESERVED') 중복이 없다 → uk_seat_hold 안전.
--    SUCCESS/PENDING = 'RESERVED', CANCELED/EXPIRED = NULL(점유 해제).
-- =============================================================================
INSERT INTO movie_reservation_seat
  (movie_reservation_seat_id, movie_reservation_id, screening_id, theater_seat_id,
   status, created_at, updated_at, created_by, updated_by)
WITH RECURSIVE k(kk) AS (SELECT 1 UNION ALL SELECT kk + 1 FROM k WHERE kk < 4)
SELECT
  ROW_NUMBER() OVER (ORDER BY r.rid, k.kk),
  r.rid,
  r.screening_id,
  (r.theater_id - 1) * 300 + (b.row_no - 1) * 20 + (b.blk - 1) * 4
    + k.kk + CASE WHEN r.seat_cnt <= 2 THEN 1 ELSE 0 END,
  CASE WHEN r.status IN ('SUCCESS', 'PENDING') THEN 'RESERVED' ELSE NULL END,
  r.created_at,
  CASE r.status
    WHEN 'CANCELED' THEN r.canceled_at
    WHEN 'EXPIRED'  THEN r.created_at + INTERVAL 5 MINUTE
    ELSE r.created_at
  END,
  r.member_id, r.member_id
FROM tmp_td_res r
JOIN tmp_td_block b ON b.rnk = r.p
JOIN k ON k.kk <= r.seat_cnt;

-- -----------------------------------------------------------------------------
-- 9-1. seat_names / original_amount 역산 (실제 배정 좌석 기준이라 불일치 불가)
-- -----------------------------------------------------------------------------
UPDATE movie_reservation r
JOIN (
  SELECT
    mrs.movie_reservation_id,
    GROUP_CONCAT(CONCAT(ts.seat_row, ts.seat_column) ORDER BY ts.theater_seat_id SEPARATOR ', ') AS seat_names,
    SUM(ts.price) AS amount
  FROM movie_reservation_seat mrs
  JOIN theater_seat ts ON ts.theater_seat_id = mrs.theater_seat_id
  GROUP BY mrs.movie_reservation_id
) agg ON agg.movie_reservation_id = r.movie_reservation_id
SET r.seat_names = agg.seat_names,
    r.original_amount = agg.amount;


-- =============================================================================
-- 10. movie_payment (DefaultEntity) — 예매 : 결제 시도 = 1 : N
--     attempt 1 이 FAILED 면 attempt 2 가 실제 결제. idempotency_key = 'PAY-{예매코드}-{시도}'.
-- =============================================================================
INSERT INTO movie_payment
  (movie_payment_id, movie_reservation_id, idempotency_key, paid_amount, status, method,
   paid_at, canceled_at, created_at, updated_at, created_by, updated_by)
SELECT
  ROW_NUMBER() OVER (ORDER BY x.rid, x.attempt),
  x.rid,
  CONCAT('PAY-', x.code, '-', x.attempt),
  x.amount,
  x.pay_status,
  x.method,
  x.paid_at,
  x.canceled_at,
  x.created_at,
  x.updated_at,
  x.member_id, x.member_id
FROM (
  -- 실패한 선행 시도 (카드 한도 초과 등)
  SELECT
    t.rid, 1 AS attempt, t.code, r.original_amount AS amount, 'FAILED' AS pay_status,
    'CARD' AS method, NULL AS paid_at, NULL AS canceled_at,
    t.created_at + INTERVAL 30 SECOND AS created_at,
    t.created_at + INTERVAL 50 SECOND AS updated_at,
    t.member_id
  FROM tmp_td_res t
  JOIN movie_reservation r ON r.movie_reservation_id = t.rid
  WHERE t.fail_before = 1
  UNION ALL
  -- 승인(COMPLETE) / 결제 후 취소(CANCELED) / 진행 중(PENDING) 시도
  SELECT
    t.rid, 1 + t.fail_before, t.code, r.original_amount,
    CASE t.status WHEN 'SUCCESS' THEN 'COMPLETE' WHEN 'CANCELED' THEN 'CANCELED' ELSE 'PENDING' END,
    t.method, t.paid_at, t.canceled_at,
    t.created_at + INTERVAL (30 + 30 * t.fail_before) SECOND,
    COALESCE(t.canceled_at, t.paid_at, t.created_at + INTERVAL (30 + 30 * t.fail_before) SECOND),
    t.member_id
  FROM tmp_td_res t
  JOIN movie_reservation r ON r.movie_reservation_id = t.rid
  WHERE t.status IN ('SUCCESS', 'CANCELED')
     OR (t.status = 'PENDING' AND t.pending_pay = 1)
) x;

-- -----------------------------------------------------------------------------
-- 10-1. 확정 결제 연결 (예매 INSERT → 결제 INSERT → 예매 UPDATE, 순환 FK)
-- -----------------------------------------------------------------------------
UPDATE movie_reservation r
JOIN movie_payment p
  ON p.movie_reservation_id = r.movie_reservation_id
 AND p.status IN ('COMPLETE', 'CANCELED')
SET r.movie_payment_id = p.movie_payment_id;


-- =============================================================================
-- 11. coupon — 10행 (DefaultEntity, is_delete soft delete)
--     issued_quantity 는 발급(§12) 후 실제 건수로 갱신한다.
--     validity_day NULL = 발급 종료일(end_at)까지 유효.
-- =============================================================================
INSERT INTO coupon
  (coupon_id, name, usage_type, discount_type, total_quantity, issued_quantity, validity_day,
   start_at, end_at, status, created_at, updated_at, created_by, updated_by, is_delete)
VALUES
  ( 1, '신규 가입 웰컴 3,000원 할인', 'MOVIE',   'MINUS_3000', 100000, 0, 30,   @now - INTERVAL 365 DAY, @now + INTERVAL 365 DAY, 'CAN_ISSUE', @now - INTERVAL 366 DAY, @now - INTERVAL 366 DAY, @admin, @admin, 0),
  ( 2, '주중 관람 10% 할인',          'MOVIE',   'PERCENT_10',  50000, 0, 14,   @now - INTERVAL 120 DAY, @now + INTERVAL 60 DAY,  'CAN_ISSUE', @now - INTERVAL 121 DAY, @now - INTERVAL 121 DAY, @admin, @admin, 0),
  ( 3, 'VIP 전용 반값 관람권',         'MOVIE',   'PERCENT_50',   5000, 0, 30,   @now - INTERVAL 120 DAY, @now + INTERVAL 60 DAY,  'CAN_ISSUE', @now - INTERVAL 121 DAY, @now - INTERVAL 121 DAY, @admin, @admin, 0),
  ( 4, '생일 축하 5,000원 할인',       'MOVIE',   'MINUS_5000', 100000, 0, NULL, @now - INTERVAL 365 DAY, @now + INTERVAL 365 DAY, 'CAN_ISSUE', @now - INTERVAL 366 DAY, @now - INTERVAL 366 DAY, @admin, @admin, 0),
  ( 5, '가을 시즌 5,000원 할인',       'MOVIE',   'MINUS_5000',      0, 0, 30,   @now - INTERVAL 60 DAY,  @now + INTERVAL 10 DAY,  'SOLD_OUT',  @now - INTERVAL 61 DAY,  @now - INTERVAL 3 DAY,   @admin, @admin, 0),
  ( 6, '스낵 콤보 3,000원 할인',       'PRODUCT', 'MINUS_3000',  30000, 0, 30,   @now - INTERVAL 120 DAY, @now + INTERVAL 60 DAY,  'CAN_ISSUE', @now - INTERVAL 121 DAY, @now - INTERVAL 121 DAY, @admin, @admin, 0),
  ( 7, '팝콘 10% 할인',                'PRODUCT', 'PERCENT_10',  30000, 0, 14,   @now - INTERVAL 120 DAY, @now + INTERVAL 60 DAY,  'CAN_ISSUE', @now - INTERVAL 121 DAY, @now - INTERVAL 121 DAY, @admin, @admin, 0),
  ( 8, '연말 특별전 반값 관람권',      'MOVIE',   'PERCENT_50',   3000, 0, 14,   @now + INTERVAL 30 DAY,  @now + INTERVAL 60 DAY,  'PRE_ISSUE', @now - INTERVAL 5 DAY,   @now - INTERVAL 5 DAY,   @admin, @admin, 0),
  ( 9, '여름 블록버스터 3,000원 할인', 'MOVIE',   'MINUS_3000',  20000, 0, 14,   @now - INTERVAL 180 DAY, @now - INTERVAL 60 DAY,  'DELETE',    @now - INTERVAL 181 DAY, @now - INTERVAL 59 DAY,  @admin, @admin, 1),
  (10, '조조 관람 3,000원 할인',       'MOVIE',   'MINUS_3000',  20000, 0, 30,   @now - INTERVAL 120 DAY, @now + INTERVAL 60 DAY,  'CAN_ISSUE', @now - INTERVAL 121 DAY, @now - INTERVAL 121 DAY, @admin, @admin, 0);


-- =============================================================================
-- 12. coupon_issuance (DefaultEntity, 물리 삭제)
--     12-1. 예매에 사용된 발급건: SUCCESS 예매의 약 8%. 예매 회원 본인에게 발급,
--           사용 1~10일 전 발급, status='USED', used_at = 결제 시각.
--           VIP 회원은 절반 확률로 VIP 반값 관람권(3) 사용.
--     12-2. 그 외 발급건: 고객 절반 + 대표 계정(1, 2)에 1건씩
--           ISSUED 60% / EXPIRED 20% / USED(매점 쿠폰, 예매 무관) 20%
-- =============================================================================
CREATE TABLE tmp_td_ci
(
  coupon_issuance_id BIGINT PRIMARY KEY,
  rid                BIGINT,
  member_id          BIGINT,
  coupon_id          BIGINT,
  created_at         DATETIME(6),
  used_at            DATETIME(6)
);
INSERT INTO tmp_td_ci
SELECT
  ROW_NUMBER() OVER (ORDER BY t.rid),
  t.rid,
  t.member_id,
  CASE WHEN m.grade = 'ROLE_VIP' AND MOD(t.rid, 2) = 0 THEN 3
       ELSE ELT(1 + MOD(t.rid, 5), 1, 2, 4, 5, 10) END,
  t.created_at - INTERVAL (1 + MOD(t.rid, 10)) DAY,
  t.paid_at
FROM tmp_td_res t
JOIN member m ON m.member_id = t.member_id
WHERE t.use_coupon = 1;

-- 12-1
INSERT INTO coupon_issuance
  (coupon_issuance_id, member_id, coupon_id, status, expired_at, used_at,
   created_at, updated_at, created_by, updated_by)
SELECT
  ci.coupon_issuance_id,
  ci.member_id,
  ci.coupon_id,
  'USED',
  COALESCE(ci.created_at + INTERVAL c.validity_day DAY, c.end_at),
  ci.used_at,
  ci.created_at,
  ci.used_at,
  ci.member_id, ci.member_id
FROM tmp_td_ci ci
JOIN coupon c ON c.coupon_id = ci.coupon_id;

-- 12-2
SET @ci_base := (SELECT COALESCE(MAX(coupon_issuance_id), 0) FROM coupon_issuance);

INSERT INTO coupon_issuance
  (coupon_issuance_id, member_id, coupon_id, status, expired_at, used_at,
   created_at, updated_at, created_by, updated_by)
SELECT
  @ci_base + ROW_NUMBER() OVER (ORDER BY y.member_id),
  y.member_id,
  y.coupon_id,
  y.st,
  y.expired_at,
  y.used_at,
  y.created_at,
  CASE y.st WHEN 'USED' THEN y.used_at WHEN 'EXPIRED' THEN y.expired_at ELSE y.created_at END,
  y.member_id, y.member_id
FROM (
  SELECT
    x.member_id, x.coupon_id, x.st,
    CASE x.st
      WHEN 'EXPIRED' THEN TIMESTAMP(@now) - INTERVAL (1 + MOD(x.member_id, 20)) DAY
      ELSE COALESCE(x.created_at + INTERVAL c.validity_day DAY, c.end_at)
    END AS expired_at,
    CASE WHEN x.st = 'USED' THEN x.created_at + INTERVAL 1 DAY END AS used_at,
    CASE x.st
      WHEN 'EXPIRED' THEN TIMESTAMP(@now) - INTERVAL (1 + MOD(x.member_id, 20)) DAY - INTERVAL c.validity_day DAY
      ELSE x.created_at
    END AS created_at
  FROM (
    SELECT
      s.member_id, s.st,
      CASE s.st
        WHEN 'ISSUED'  THEN CASE WHEN s.grade = 'ROLE_VIP' THEN 3
                                 ELSE ELT(1 + MOD(s.member_id, 5), 1, 2, 4, 6, 10) END
        WHEN 'EXPIRED' THEN ELT(1 + MOD(s.member_id, 3), 2, 6, 7)       -- 유효기간이 정해진 쿠폰
        ELSE ELT(1 + MOD(s.member_id, 2), 6, 7)                         -- 매점 쿠폰 사용
      END AS coupon_id,
      CASE s.st
        WHEN 'ISSUED' THEN TIMESTAMP(@now) - INTERVAL (1 + MOD(s.member_id, 10)) DAY
        ELSE TIMESTAMP(@now) - INTERVAL (3 + MOD(s.member_id, 10)) DAY
      END AS created_at
    FROM (
      SELECT
        cu.member_id, cu.grade,
        CASE
          WHEN cu.member_id IN (1, 2)        THEN 'ISSUED'
          WHEN MOD(cu.member_id * 7, 10) < 6 THEN 'ISSUED'
          WHEN MOD(cu.member_id * 7, 10) < 8 THEN 'EXPIRED'
          ELSE 'USED'
        END AS st
      FROM tmp_td_cust cu
      WHERE MOD(cu.rn, 2) = 0 OR cu.member_id IN (1, 2)
    ) s
  ) x
  JOIN coupon c ON c.coupon_id = x.coupon_id
) y;

-- -----------------------------------------------------------------------------
-- 12-3. 발급 수량 반영 (매진 쿠폰은 총 수량 = 발급 수량) / 예매-쿠폰 연결
-- -----------------------------------------------------------------------------
UPDATE coupon c
LEFT JOIN (SELECT coupon_id, COUNT(*) AS n FROM coupon_issuance GROUP BY coupon_id) i
  ON i.coupon_id = c.coupon_id
SET c.issued_quantity = COALESCE(i.n, 0),
    c.total_quantity  = CASE WHEN c.status = 'SOLD_OUT' THEN COALESCE(i.n, 0) ELSE c.total_quantity END;

UPDATE movie_reservation r
JOIN tmp_td_ci ci ON ci.rid = r.movie_reservation_id
SET r.coupon_issuance_id = ci.coupon_issuance_id;


-- =============================================================================
-- 13. file_master (DefaultEntity, is_delete soft delete)
--     영화마다 포스터(THUMBNAIL) 1건 + 회차가 있는 영화(weight > 0)는 스틸컷(IMAGE) 2건.
--     target_type='MOVIE', target_id=movie_id (polymorphic 참조라 물리 FK 없음)
-- =============================================================================
INSERT INTO file_master
  (file_id, target_id, target_type, usage_type, original_name, destination, size, content_type, view_order,
   created_at, updated_at, created_by, updated_by, is_delete)
SELECT
  ROW_NUMBER() OVER (ORDER BY m.movie_id, u.ord),
  m.movie_id,
  'MOVIE',
  CASE WHEN u.ord = 1 THEN 'THUMBNAIL' ELSE 'IMAGE' END,
  CASE WHEN u.ord = 1 THEN CONCAT('poster_', m.movie_id, '.jpg')
       ELSE CONCAT('still_', m.movie_id, '_', u.ord - 1, '.jpg') END,
  CASE WHEN u.ord = 1 THEN CONCAT('https://cdn.example.com/movie/', m.movie_id, '/poster.jpg')
       ELSE CONCAT('https://cdn.example.com/movie/', m.movie_id, '/still_', u.ord - 1, '.jpg') END,
  CASE WHEN u.ord = 1 THEN 350000 + MOD(m.movie_id * 7919, 250000)
       ELSE 800000 + MOD(m.movie_id * 104729 + u.ord, 700000) END,
  'image/jpeg',
  CASE WHEN u.ord = 1 THEN 1 ELSE u.ord - 1 END,
  @now - INTERVAL (GREATEST(30 - m.rel_off, 30)) DAY,
  @now - INTERVAL (GREATEST(30 - m.rel_off, 30)) DAY,
  @admin, @admin, 0
FROM tmp_td_movie m
JOIN (SELECT 1 AS ord UNION ALL SELECT 2 UNION ALL SELECT 3) u
  ON u.ord = 1 OR m.weight > 0;


-- =============================================================================
-- 14. common_code_group / common_code_detail (DefaultEntity, is_delete soft delete)
--     enum 을 미러링한 코드 + 비즈니스 코드(예매 취소 사유). attr 는 부가 값.
-- =============================================================================
INSERT INTO common_code_group
  (common_code_group_id, group_name, description, created_at, updated_at, created_by, updated_by, is_delete)
VALUES
  ('MEMBER_GRADE',       '회원 등급',      'MemberGrade',       @now, @now, @admin, @admin, 0),
  ('MEMBER_STATUS',      '회원 상태',      'MemberStatus',      @now, @now, @admin, @admin, 0),
  ('SOCIAL_PROVIDER',    '가입 경로',      'SocialProvider',    @now, @now, @admin, @admin, 0),
  ('MOVIE_CATEGORY',     '영화 장르',      'MovieCategory',     @now, @now, @admin, @admin, 0),
  ('MOVIE_AGE_RATING',   '관람 등급',      'MovieAgeRating',    @now, @now, @admin, @admin, 0),
  ('SCREEN_TYPE',        '상영 타입',      'ScreenType',        @now, @now, @admin, @admin, 0),
  ('SEAT_GRADE',         '좌석 등급',      'SeatGrade (attr=가격)', @now, @now, @admin, @admin, 0),
  ('SCREENING_STATUS',   '회차 판매 상태', 'ScreeningStatus',   @now, @now, @admin, @admin, 0),
  ('RESERVATION_STATUS', '예매 상태',      'ReservationStatus', @now, @now, @admin, @admin, 0),
  ('PAYMENT_METHOD',     '결제 수단',      'PaymentMethod',     @now, @now, @admin, @admin, 0),
  ('PAYMENT_STATUS',     '결제 상태',      'PaymentStatus',     @now, @now, @admin, @admin, 0),
  ('DISCOUNT_TYPE',      '쿠폰 할인 종류', 'DiscountType (attr=할인 값)', @now, @now, @admin, @admin, 0),
  ('ISSUANCE_STATUS',    '쿠폰 발급 상태', 'IssuanceStatus',    @now, @now, @admin, @admin, 0),
  ('CANCEL_REASON',      '예매 취소 사유', '고객 예매 취소 시 선택하는 사유', @now, @now, @admin, @admin, 0);

INSERT INTO common_code_detail
  (common_code_group_id, common_code_detail_code, display_name, description, sort_order, attr,
   created_at, updated_at, created_by, updated_by, is_delete)
VALUES
  ('MEMBER_GRADE', 'ROLE_COMMON',       '일반',        '일반 회원',            1, NULL, @now, @now, @admin, @admin, 0),
  ('MEMBER_GRADE', 'ROLE_VIP',          'VIP',         'VIP 회원',             2, NULL, @now, @now, @admin, @admin, 0),
  ('MEMBER_GRADE', 'ROLE_MANAGE_ADMIN', '운영 관리자', '영화·회차 운영 관리자', 3, NULL, @now, @now, @admin, @admin, 0),
  ('MEMBER_GRADE', 'ROLE_SUPER_ADMIN',  '최고 관리자', '전체 권한 관리자',      4, NULL, @now, @now, @admin, @admin, 0),

  ('MEMBER_STATUS', 'ACTIVE', '정상', '정상 이용 회원',            1, NULL, @now, @now, @admin, @admin, 0),
  ('MEMBER_STATUS', 'SLEEP',  '휴면', '1년 이상 미접속 휴면 회원', 2, NULL, @now, @now, @admin, @admin, 0),
  ('MEMBER_STATUS', 'LOCKED', '잠금', '로그인 실패 등으로 잠긴 회원', 3, NULL, @now, @now, @admin, @admin, 0),
  ('MEMBER_STATUS', 'DELETE', '탈퇴', '탈퇴 회원(soft delete)',    4, NULL, @now, @now, @admin, @admin, 0),

  ('SOCIAL_PROVIDER', 'NORMAL', '이메일', '이메일 가입', 1, NULL, @now, @now, @admin, @admin, 0),
  ('SOCIAL_PROVIDER', 'KAKAO',  '카카오', '카카오 로그인', 2, NULL, @now, @now, @admin, @admin, 0),
  ('SOCIAL_PROVIDER', 'GOOGLE', '구글',   '구글 로그인',   3, NULL, @now, @now, @admin, @admin, 0),

  ('MOVIE_CATEGORY', 'ROMANCE',  '로맨스', '로맨스·멜로',   1, NULL, @now, @now, @admin, @admin, 0),
  ('MOVIE_CATEGORY', 'THRILLER', '스릴러', '스릴러·범죄',   2, NULL, @now, @now, @admin, @admin, 0),
  ('MOVIE_CATEGORY', 'SF',       'SF',     'SF·판타지',     3, NULL, @now, @now, @admin, @admin, 0),

  ('MOVIE_AGE_RATING', 'ALL',        '전체 관람가',      '모든 연령 관람 가능',  1, '0',  @now, @now, @admin, @admin, 0),
  ('MOVIE_AGE_RATING', 'UPPER_12',   '12세 이상 관람가', '만 12세 이상 관람 가능', 2, '12', @now, @now, @admin, @admin, 0),
  ('MOVIE_AGE_RATING', 'UPPER_15',   '15세 이상 관람가', '만 15세 이상 관람 가능', 3, '15', @now, @now, @admin, @admin, 0),
  ('MOVIE_AGE_RATING', 'ONLY_ADULT', '청소년 관람불가',  '만 19세 이상 관람 가능', 4, '19', @now, @now, @admin, @admin, 0),

  ('SCREEN_TYPE', 'V_2D',      '2D',      '일반 2D 상영',          1, NULL, @now, @now, @admin, @admin, 0),
  ('SCREEN_TYPE', 'V_4D',      '4D',      '4DX 모션 체어 상영',    2, NULL, @now, @now, @admin, @admin, 0),
  ('SCREEN_TYPE', 'V_SCREENX', 'SCREENX', '3면 스크린 상영',       3, NULL, @now, @now, @admin, @admin, 0),

  ('SEAT_GRADE', 'NORMAL',   '일반석',     '일반 좌석 (A~L열)',     1, '8000',  @now, @now, @admin, @admin, 0),
  ('SEAT_GRADE', 'ADVANCED', '프리미엄석', '프리미엄 좌석 (M~O열)', 2, '10000', @now, @now, @admin, @admin, 0),

  ('SCREENING_STATUS', 'PRE_SALE', '오픈 예정', '예매 오픈 전',  1, NULL, @now, @now, @admin, @admin, 0),
  ('SCREENING_STATUS', 'ON_SALE',  '예매 중',   '예매 진행 중',  2, NULL, @now, @now, @admin, @admin, 0),
  ('SCREENING_STATUS', 'SOLD_OUT', '매진',      '전 좌석 매진',  3, NULL, @now, @now, @admin, @admin, 0),
  ('SCREENING_STATUS', 'END_SALE', '예매 종료', '상영 시작으로 예매 종료', 4, NULL, @now, @now, @admin, @admin, 0),

  ('RESERVATION_STATUS', 'PENDING',  '결제 대기', '좌석 홀드, 결제 전(5분)', 1, NULL, @now, @now, @admin, @admin, 0),
  ('RESERVATION_STATUS', 'SUCCESS',  '예매 완료', '결제 완료',              2, NULL, @now, @now, @admin, @admin, 0),
  ('RESERVATION_STATUS', 'CANCELED', '예매 취소', '결제 후 고객 취소',      3, NULL, @now, @now, @admin, @admin, 0),
  ('RESERVATION_STATUS', 'EXPIRED',  '시간 만료', '홀드 시간 내 미결제',    4, NULL, @now, @now, @admin, @admin, 0),

  ('PAYMENT_METHOD', 'CARD',  '신용/체크카드', '카드 결제',     1, NULL, @now, @now, @admin, @admin, 0),
  ('PAYMENT_METHOD', 'KAKAO', '카카오페이',    '카카오페이 결제', 2, NULL, @now, @now, @admin, @admin, 0),
  ('PAYMENT_METHOD', 'TOSS',  '토스페이',      '토스페이 결제', 3, NULL, @now, @now, @admin, @admin, 0),

  ('PAYMENT_STATUS', 'PENDING',  '결제 진행 중', 'PG 승인 대기', 1, NULL, @now, @now, @admin, @admin, 0),
  ('PAYMENT_STATUS', 'COMPLETE', '결제 완료',    'PG 승인 완료', 2, NULL, @now, @now, @admin, @admin, 0),
  ('PAYMENT_STATUS', 'FAILED',   '결제 실패',    'PG 승인 실패', 3, NULL, @now, @now, @admin, @admin, 0),
  ('PAYMENT_STATUS', 'CANCELED', '결제 취소',    '승인 후 취소(환불)', 4, NULL, @now, @now, @admin, @admin, 0),

  ('DISCOUNT_TYPE', 'PERCENT_10', '10% 할인',    '결제 금액의 10% 할인', 1, '10',   @now, @now, @admin, @admin, 0),
  ('DISCOUNT_TYPE', 'PERCENT_50', '50% 할인',    '결제 금액의 50% 할인', 2, '50',   @now, @now, @admin, @admin, 0),
  ('DISCOUNT_TYPE', 'MINUS_3000', '3,000원 할인', '3,000원 정액 할인',   3, '3000', @now, @now, @admin, @admin, 0),
  ('DISCOUNT_TYPE', 'MINUS_5000', '5,000원 할인', '5,000원 정액 할인',   4, '5000', @now, @now, @admin, @admin, 0),

  ('ISSUANCE_STATUS', 'ISSUED',  '사용 가능', '발급 후 미사용', 1, NULL, @now, @now, @admin, @admin, 0),
  ('ISSUANCE_STATUS', 'USED',    '사용 완료', '사용 처리됨',    2, NULL, @now, @now, @admin, @admin, 0),
  ('ISSUANCE_STATUS', 'EXPIRED', '기간 만료', '유효기간 경과',  3, NULL, @now, @now, @admin, @admin, 0),

  ('CANCEL_REASON', 'SCHEDULE', '일정 변경',   '관람 일정이 바뀜',       1, NULL, @now, @now, @admin, @admin, 0),
  ('CANCEL_REASON', 'MISTAKE',  '잘못 예매',   '회차·좌석을 잘못 선택',  2, NULL, @now, @now, @admin, @admin, 0),
  ('CANCEL_REASON', 'RESEAT',   '좌석 변경',   '다른 좌석으로 재예매',   3, NULL, @now, @now, @admin, @admin, 0),
  ('CANCEL_REASON', 'ETC',      '기타',        '기타 사유',              4, NULL, @now, @now, @admin, @admin, 0);


-- -----------------------------------------------------------------------------
-- 보조 테이블 정리
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS tmp_td_movie, tmp_td_scr, tmp_td_cust, tmp_td_block, tmp_td_res, tmp_td_ci;

SET FOREIGN_KEY_CHECKS = 1;
