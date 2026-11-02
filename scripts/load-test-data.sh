#!/usr/bin/env bash
# =============================================================================
# cinema-middleware 테스트 데이터 적재 스크립트
#
#   1. docker compose 인프라(MySQL) 기동 확인
#   2. 테이블이 15개 미만이면 schema.sql 적재 (기존 테이블 구조는 바꾸지 않음 — 스키마 변경은 수동 반영)
#   3. docs/test-data.sql 적재 (내부에서 TRUNCATE 후 INSERT → 몇 번 실행해도 동일)
#   4. 행수 · 좌석/예매/결제/쿠폰 정합성 · FK 무결성 검증 출력
#
# 사용법:
#   bash scripts/load-test-data.sh
#   BASE_DATE=2026-12-25 bash scripts/load-test-data.sh   # 기준일 변경
#
# 환경변수 (미지정 시 docker-compose.yml 기본값):
#   DB_NAME(cinema) DB_USER(cinema_admin) DB_PASS
#   COMPOSE_SERVICE(mysql) BASE_DATE(미지정 시 test-data.sql 파일 기본값 2026-11-01)
# =============================================================================
set -euo pipefail

DB_NAME="${DB_NAME:-cinema}"
DB_USER="${DB_USER:-cinema_admin}"
DB_PASS="${DB_PASS:-cinema_password}"
COMPOSE_SERVICE="${COMPOSE_SERVICE:-mysql}"
BASE_DATE="${BASE_DATE:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

SCHEMA_SQL="src/main/resources/schema.sql"
DATA_SQL="docs/test-data.sql"

log() { printf '\033[1;34m[load-test-data]\033[0m %s\n' "$*"; }
err() { printf '\033[1;31m[load-test-data][에러]\033[0m %s\n' "$*" >&2; }

[ -f "$DATA_SQL" ]   || { err "$DATA_SQL 없음"; exit 1; }
[ -f "$SCHEMA_SQL" ] || { err "$SCHEMA_SQL 없음"; exit 1; }

# -----------------------------------------------------------------------------
# 1. 인프라 확인
# -----------------------------------------------------------------------------
if ! docker compose ps --status running --services 2>/dev/null | grep -qx "$COMPOSE_SERVICE"; then
  log "MySQL 컨테이너 미기동 → docker compose up -d $COMPOSE_SERVICE"
  docker compose up -d "$COMPOSE_SERVICE"
fi

# -----------------------------------------------------------------------------
# 2. MySQL 응답 대기 (최대 30초)
# -----------------------------------------------------------------------------
log "MySQL 응답 대기..."
for i in $(seq 1 30); do
  if docker compose exec -T "$COMPOSE_SERVICE" \
       mysqladmin ping -h localhost -uroot -proot --silent 2>/dev/null; then
    break
  fi
  [ "$i" -eq 30 ] && { err "MySQL 30초 내 무응답"; exit 1; }
  sleep 1
done

mysql_exec() {
  docker compose exec -T "$COMPOSE_SERVICE" \
    mysql --default-character-set=utf8mb4 -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" "$@"
}

# -----------------------------------------------------------------------------
# 3. 테이블 존재 확인 → 부족하면 schema.sql
# -----------------------------------------------------------------------------
TABLE_CNT="$(
  printf "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='%s';" "$DB_NAME" \
  | mysql_exec -N 2>/dev/null || echo 0
)"
if [ "${TABLE_CNT:-0}" -lt 15 ]; then
  log "테이블 부족 (${TABLE_CNT}/15) → schema.sql 적재"
  mysql_exec < "$SCHEMA_SQL"
fi

# -----------------------------------------------------------------------------
# 4. 데이터 적재 (BASE_DATE 지정 시 기준일 두 줄 치환)
# -----------------------------------------------------------------------------
LOAD_SQL="$DATA_SQL"
TMP_SQL=""
if [ -n "$BASE_DATE" ]; then
  if ! [[ "$BASE_DATE" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
    err "BASE_DATE 형식은 YYYY-MM-DD 여야 함 (받은 값: $BASE_DATE)"
    exit 1
  fi
  TMP_SQL="$(mktemp -t cinema-test-data.XXXXXX.sql)"
  # shellcheck disable=SC2064
  trap "rm -f '$TMP_SQL'" EXIT
  sed -E \
    -e "s/^SET @today := DATE '[0-9-]+';/SET @today := DATE '${BASE_DATE}';/" \
    -e "s/^SET @now   := TIMESTAMP '[0-9-]+ [0-9:]+';/SET @now   := TIMESTAMP '${BASE_DATE} 12:00:00';/" \
    "$DATA_SQL" > "$TMP_SQL"
  LOAD_SQL="$TMP_SQL"
  log "기준일 오버라이드: $BASE_DATE"
fi

log "테스트 데이터 적재: $DATA_SQL"
mysql_exec < "$LOAD_SQL" || { err "적재 실패"; exit 1; }

# -----------------------------------------------------------------------------
# 5. 검증
# -----------------------------------------------------------------------------
log "행수:"
mysql_exec -t <<'SQL'
SELECT 'member' tbl, COUNT(*) cnt FROM member
UNION ALL SELECT 'movie', COUNT(*) FROM movie
UNION ALL SELECT 'cinema', COUNT(*) FROM cinema
UNION ALL SELECT 'theater', COUNT(*) FROM theater
UNION ALL SELECT 'theater_seat', COUNT(*) FROM theater_seat
UNION ALL SELECT 'screening', COUNT(*) FROM screening
UNION ALL SELECT 'movie_reservation', COUNT(*) FROM movie_reservation
UNION ALL SELECT 'movie_reservation_seat', COUNT(*) FROM movie_reservation_seat
UNION ALL SELECT 'movie_payment', COUNT(*) FROM movie_payment
UNION ALL SELECT 'coupon', COUNT(*) FROM coupon
UNION ALL SELECT 'coupon_issuance', COUNT(*) FROM coupon_issuance
UNION ALL SELECT 'file_master', COUNT(*) FROM file_master
UNION ALL SELECT 'common_code_group', COUNT(*) FROM common_code_group
UNION ALL SELECT 'common_code_detail', COUNT(*) FROM common_code_detail;
SQL

log "정합성 검사 (v 컬럼이 전부 0이어야 정상):"
mysql_exec -t <<'SQL'
-- 좌석 / 회차
SELECT 'seat_per_theater_not_300' k, COUNT(*) v FROM (SELECT theater_id FROM theater_seat GROUP BY theater_id HAVING COUNT(*) <> 300) x
UNION ALL SELECT 'seat_price_grade_mismatch', COUNT(*) FROM theater_seat WHERE (grade='NORMAL' AND price<>8000) OR (grade='ADVANCED' AND price<>10000)
UNION ALL SELECT 'uk_seat_hold_dup', COUNT(*) FROM (SELECT 1 FROM movie_reservation_seat WHERE status='RESERVED' GROUP BY screening_id, theater_seat_id HAVING COUNT(*) > 1) x
UNION ALL SELECT 'seat_other_theater', COUNT(*) FROM movie_reservation_seat mrs JOIN screening s ON s.screening_id=mrs.screening_id JOIN theater_seat ts ON ts.theater_seat_id=mrs.theater_seat_id WHERE ts.theater_id<>s.theater_id
UNION ALL SELECT 'screening_out_of_run', COUNT(*) FROM screening s JOIN movie m ON m.movie_id=s.movie_id WHERE s.start_date NOT BETWEEN m.release_date AND m.close_date
UNION ALL SELECT 'screening_hidden_movie', COUNT(*) FROM screening s JOIN movie m ON m.movie_id=s.movie_id WHERE m.status='HIDDEN'
UNION ALL SELECT 'screening_closed_theater', COUNT(*) FROM screening s JOIN theater t ON t.theater_id=s.theater_id WHERE t.status<>'AVAILABLE'
UNION ALL SELECT 'sold_out_not_full', COUNT(*) FROM screening s WHERE s.status='SOLD_OUT' AND (SELECT COUNT(*) FROM movie_reservation_seat x WHERE x.screening_id=s.screening_id AND x.status='RESERVED') <> 300
UNION ALL SELECT 'pre_sale_has_reservation', COUNT(*) FROM movie_reservation r JOIN screening s ON s.screening_id=r.screening_id WHERE s.status='PRE_SALE'
-- 예매 / 결제
UNION ALL SELECT 'res_seat_count_out_of_1_4', COUNT(*) FROM (SELECT movie_reservation_id FROM movie_reservation_seat GROUP BY movie_reservation_id HAVING COUNT(*) NOT BETWEEN 1 AND 4) x
UNION ALL SELECT 'seat_names_count_mismatch', COUNT(*) FROM movie_reservation r WHERE (LENGTH(r.seat_names) - LENGTH(REPLACE(r.seat_names, ', ', ''))) / 2 + 1 <> (SELECT COUNT(*) FROM movie_reservation_seat x WHERE x.movie_reservation_id=r.movie_reservation_id)
UNION ALL SELECT 'original_amount_mismatch', COUNT(*) FROM movie_reservation r WHERE r.original_amount <> (SELECT SUM(ts.price) FROM movie_reservation_seat x JOIN theater_seat ts ON ts.theater_seat_id=x.theater_seat_id WHERE x.movie_reservation_id=r.movie_reservation_id)
UNION ALL SELECT 'seat_status_mismatch', COUNT(*) FROM movie_reservation_seat x JOIN movie_reservation r ON r.movie_reservation_id=x.movie_reservation_id WHERE (r.status IN ('SUCCESS','PENDING')) <> (x.status <=> 'RESERVED')
UNION ALL SELECT 'success_without_complete_pay', COUNT(*) FROM movie_reservation r LEFT JOIN movie_payment p ON p.movie_payment_id=r.movie_payment_id WHERE r.status='SUCCESS' AND (p.status IS NULL OR p.status<>'COMPLETE')
UNION ALL SELECT 'paid_amount_mismatch', COUNT(*) FROM movie_payment p JOIN movie_reservation r ON r.movie_reservation_id=p.movie_reservation_id WHERE p.paid_amount<>r.original_amount
UNION ALL SELECT 'pending_already_expired', COUNT(*) FROM movie_reservation WHERE status='PENDING' AND expired_at <= (SELECT MAX(created_at) FROM movie_reservation)
-- 쿠폰
UNION ALL SELECT 'coupon_used_by_other_member', COUNT(*) FROM movie_reservation r JOIN coupon_issuance ci ON ci.coupon_issuance_id=r.coupon_issuance_id WHERE ci.member_id<>r.member_id OR ci.status<>'USED'
UNION ALL SELECT 'issuance_used_twice', COUNT(*) FROM (SELECT coupon_issuance_id FROM movie_reservation WHERE coupon_issuance_id IS NOT NULL GROUP BY coupon_issuance_id HAVING COUNT(*) > 1) x
UNION ALL SELECT 'issued_quantity_mismatch', COUNT(*) FROM coupon c WHERE c.issued_quantity <> (SELECT COUNT(*) FROM coupon_issuance i WHERE i.coupon_id=c.coupon_id) OR c.issued_quantity > c.total_quantity
-- FK 고아행
UNION ALL SELECT 'orphan_reservation_member', COUNT(*) FROM movie_reservation r LEFT JOIN member m ON m.member_id=r.member_id WHERE m.member_id IS NULL OR m.status<>'ACTIVE'
UNION ALL SELECT 'orphan_mrs_reservation', COUNT(*) FROM movie_reservation_seat x LEFT JOIN movie_reservation r ON r.movie_reservation_id=x.movie_reservation_id WHERE r.movie_reservation_id IS NULL
UNION ALL SELECT 'orphan_payment_reservation', COUNT(*) FROM movie_payment p LEFT JOIN movie_reservation r ON r.movie_reservation_id=p.movie_reservation_id WHERE r.movie_reservation_id IS NULL
UNION ALL SELECT 'orphan_issuance_coupon', COUNT(*) FROM coupon_issuance i LEFT JOIN coupon c ON c.coupon_id=i.coupon_id WHERE c.coupon_id IS NULL
UNION ALL SELECT 'orphan_file_movie', COUNT(*) FROM file_master f LEFT JOIN movie m ON m.movie_id=f.target_id WHERE f.target_type='MOVIE' AND m.movie_id IS NULL
UNION ALL SELECT 'leftover_tmp_tables', COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name LIKE 'tmp_td_%';
SQL

log "회차 상태 / 예매 상태 / 결제 상태 분포:"
mysql_exec -t <<'SQL'
SELECT 'screening' kind, status, COUNT(*) cnt FROM screening GROUP BY status
UNION ALL SELECT 'reservation', status, COUNT(*) FROM movie_reservation GROUP BY status
UNION ALL SELECT 'payment', status, COUNT(*) FROM movie_payment GROUP BY status
UNION ALL SELECT 'issuance', status, COUNT(*) FROM coupon_issuance GROUP BY status
ORDER BY kind, cnt DESC;
SQL

log "완료. 로그인 계정: user@ / vip@ / admin@ / super@example.com (비밀번호 test1234!)"
