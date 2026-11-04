#!/usr/bin/env bash
# =============================================================================
# seat-race 부하 테스트 데이터 정리 — 테스트가 추가한 데이터를 로컬 MySQL·Redis 에서 모두 지운다
#
#   run.sh 가 테스트 종료 시(성공·실패 모두) 자동 호출한다. 단독 실행도 가능(남은 데이터 일괄 정리).
#   관리자 API 는 예매가 남은 회차 삭제를 거부(code 17)하므로 DB 에서 직접 hard delete 한다.
#
# 정리 대상(표식으로만 찾는다 — 표식이 없는 실제 데이터는 건드리지 않는다)
#   - 상영관: 이름이 'LOADTEST-seat-race-' 로 시작(common.sh 표식) → 그 좌석·회차
#   - 회원: 이메일 loadtest-<RUN_TAG>-NNNNNN@example.com (seed-members.sh 가 만든 형식)
#   - 위 회차·회원의 예매·예매 좌석·결제·예매/결제 이력, 회원의 쿠폰 발급
#   - Redis: 위 회원의 refresh 토큰 키(refresh:<member_id>)
#
# 사용법:
#   DB_PASS=... bash scripts/load/seat-race/reset.sh               # 모든 실행의 데이터 정리
#   DB_PASS=... bash scripts/load/seat-race/reset.sh --dry-run     # 건수만 확인
#
# 환경변수:
#   DB_PASS(필수, docker-compose.yml 의 MYSQL_PASSWORD) DB_NAME(cinema) DB_USER(cinema_admin)
#   MYSQL_SERVICE(mysql) REDIS_SERVICE(redis)
#
# DB 비밀번호는 이 파일에 쓰지 않는다. 환경변수로만 받는다.
# =============================================================================
# -e: 명령 실패 시 중단, -u: 미정의 변수 사용 시 중단, -o pipefail: 파이프 중간 실패도 실패로 처리
set -euo pipefail

# 저장소 루트로 이동(docker-compose.yml 위치)
cd "$(dirname "$0")/../../.."
# shellcheck source=../common.sh
source scripts/load/common.sh

# ----- 설정·인자 -----
DB_NAME="${DB_NAME:-cinema}"
DB_USER="${DB_USER:-cinema_admin}"
MYSQL_SERVICE="${MYSQL_SERVICE:-mysql}"
REDIS_SERVICE="${REDIS_SERVICE:-redis}"
: "${DB_PASS:?DB_PASS 를 지정하세요(docker-compose.yml 의 MYSQL_PASSWORD)}"
DRY_RUN=0
case "${1:-}" in
  '') ;;
  --dry-run) DRY_RUN=1 ;;
  *) fail "사용법: reset.sh [--dry-run]"; exit 1 ;;
esac
for v in DB_NAME DB_USER MYSQL_SERVICE REDIS_SERVICE; do
  [[ "${!v}" =~ ^[A-Za-z0-9_-]+$ ]] || { fail "$v 형식 오류: ${!v}"; exit 1; }
done
# 상영관 표식은 SQL 문자열·LIKE 에 들어가므로 영문·숫자·'-' 만 허용한다('_'·'%' 와일드카드 차단)
[[ "$LOADTEST_THEATER_PREFIX" =~ ^[A-Za-z0-9-]+$ ]] \
  || { fail "LOADTEST_THEATER_PREFIX 형식 오류: $LOADTEST_THEATER_PREFIX"; exit 1; }

# MySQL 컨테이너에서 stdin 의 SQL 실행(-N -B: 머리글 없는 탭 구분). 비밀번호 경고 줄은 숨긴다
mysql_exec() {
  docker compose exec -T "$MYSQL_SERVICE" \
    mysql --default-character-set=utf8mb4 -N -B -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" \
    2> >(grep -v 'Using a password on the command line' >&2)
}

# 대상 id 를 임시 테이블에 모으는 SQL. 같은 세션 안에서만 유효하다
TARGET_SQL="
CREATE TEMPORARY TABLE t_theater AS
  SELECT theater_id FROM theater WHERE name LIKE '${LOADTEST_THEATER_PREFIX}%';
CREATE TEMPORARY TABLE t_screening AS
  SELECT screening_id FROM screening WHERE theater_id IN (SELECT theater_id FROM t_theater);
CREATE TEMPORARY TABLE t_member AS
  SELECT member_id FROM member WHERE email REGEXP '^loadtest-[a-z0-9]{1,20}-[0-9]{6}@example[.]com\$';
CREATE TEMPORARY TABLE t_reservation AS
  SELECT movie_reservation_id FROM movie_reservation
  WHERE screening_id IN (SELECT screening_id FROM t_screening)
     OR member_id IN (SELECT member_id FROM t_member);
"

# 대상 건수 → 전역 COUNTS(탭 구분: 상영관 회차 좌석 회원 예매 예매좌석 결제)
# MySQL 은 한 쿼리에서 같은 임시 테이블을 두 번 열 수 없어 SELECT 를 건수마다 나눈다
count_targets() {
  COUNTS="$(printf '%s\n%s\n' "$TARGET_SQL" "
SELECT COUNT(*) FROM t_theater;
SELECT COUNT(*) FROM t_screening;
SELECT COUNT(*) FROM theater_seat WHERE theater_id IN (SELECT theater_id FROM t_theater);
SELECT COUNT(*) FROM t_member;
SELECT COUNT(*) FROM t_reservation;
SELECT COUNT(*) FROM movie_reservation_seat
  WHERE movie_reservation_id IN (SELECT movie_reservation_id FROM t_reservation)
     OR screening_id IN (SELECT screening_id FROM t_screening);
SELECT COUNT(*) FROM movie_payment
  WHERE movie_reservation_id IN (SELECT movie_reservation_id FROM t_reservation);" \
    | mysql_exec | paste -sd '\t' -)" || { fail "대상 조회 실패(DB_USER·DB_PASS 확인)"; exit 1; }
  [[ "$COUNTS" =~ ^[0-9]+([[:space:]][0-9]+){6}$ ]] || { fail "대상 조회 결과 형식 오류: $COUNTS"; exit 1; }
}

print_counts() {
  local c
  read -r -a c <<<"$COUNTS"
  info "상영관 ${c[0]} / 회차 ${c[1]} / 좌석 ${c[2]} / 회원 ${c[3]} / 예매 ${c[4]} / 예매 좌석 ${c[5]} / 결제 ${c[6]}"
}

# ----- 1. 사전 점검 -----
header "정리 — 사전 점검"
require_cmd docker
RUNNING="$(docker compose ps --status running --services 2>/dev/null || true)"
for s in "$MYSQL_SERVICE" "$REDIS_SERVICE"; do
  grep -qx "$s" <<<"$RUNNING" || { fail "컨테이너($s)가 실행 중이 아닙니다: docker compose up -d"; exit 1; }
done

# ----- 2. 대상 확인 -----
header "정리 — 대상"
count_targets
print_counts
if [ "$DRY_RUN" = "1" ]; then
  info "--dry-run: 지우지 않고 종료합니다"
  exit 0
fi
if [ "$COUNTS" = "$(printf '0\t0\t0\t0\t0\t0\t0')" ]; then
  pass "정리할 부하 테스트 데이터가 없습니다"
  exit 0
fi

# ----- 3. Redis refresh 토큰 삭제 (회원을 지우기 전에 id 를 얻는다) -----
header "정리 — Redis refresh 토큰"
# 회원 id 마다 'DEL refresh:<id>' 명령을 만들어 redis-cli 에 한 번에 흘려보낸다
deleted="$(printf '%s\n%s\n' "$TARGET_SQL" "SELECT CONCAT('DEL refresh:', member_id) FROM t_member;" \
  | mysql_exec | docker compose exec -T "$REDIS_SERVICE" redis-cli \
  | awk '{ s += $1 } END { print s + 0 }')" || { fail "Redis 토큰 삭제 실패"; exit 1; }
info "refresh 토큰 ${deleted}개 삭제"

# ----- 4. DB 삭제 (한 트랜잭션, FK 순서) -----
header "정리 — DB 삭제"
started=$SECONDS
# 예매 ↔ 결제가 서로 참조하므로 예매의 결제 FK 를 먼저 끊고 자식 → 부모 순서로 지운다
printf '%s\n%s\n' "$TARGET_SQL" "
START TRANSACTION;
DELETE FROM movie_payment_history
  WHERE movie_reservation_id IN (SELECT movie_reservation_id FROM t_reservation);
DELETE FROM movie_reservation_history
  WHERE movie_reservation_id IN (SELECT movie_reservation_id FROM t_reservation);
UPDATE movie_reservation SET movie_payment_id = NULL
  WHERE movie_reservation_id IN (SELECT movie_reservation_id FROM t_reservation);
DELETE FROM movie_payment
  WHERE movie_reservation_id IN (SELECT movie_reservation_id FROM t_reservation);
DELETE FROM movie_reservation_seat
  WHERE movie_reservation_id IN (SELECT movie_reservation_id FROM t_reservation)
     OR screening_id IN (SELECT screening_id FROM t_screening);
DELETE FROM movie_reservation
  WHERE movie_reservation_id IN (SELECT movie_reservation_id FROM t_reservation);
DELETE FROM coupon_issuance WHERE member_id IN (SELECT member_id FROM t_member);
DELETE FROM screening WHERE screening_id IN (SELECT screening_id FROM t_screening);
DELETE FROM theater_seat WHERE theater_id IN (SELECT theater_id FROM t_theater);
DELETE FROM theater WHERE theater_id IN (SELECT theater_id FROM t_theater);
DELETE FROM member WHERE member_id IN (SELECT member_id FROM t_member);
COMMIT;" | mysql_exec || { fail "DB 삭제 실패(트랜잭션 롤백됨)"; exit 1; }
info "삭제 소요 $((SECONDS - started))초"

# ----- 5. 결과 확인: 모든 대상이 0건이어야 한다 -----
header "정리 — 결과 확인"
count_targets
print_counts
if [ "$COUNTS" = "$(printf '0\t0\t0\t0\t0\t0\t0')" ]; then
  pass "부하 테스트 데이터 정리 완료"
  exit 0
fi
fail "남은 데이터가 있습니다(위 건수 참고)"
exit 1
