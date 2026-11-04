#!/usr/bin/env bash
# =============================================================================
# seat-race 부하 테스트 회원 일괄 적재 (로컬 MySQL 직접 INSERT, docker compose exec -T)
#
#   실행마다 새 회원을 만든다: loadtest-<RUN_TAG>-000001@example.com ~ MEMBER_COUNT 번
#   (ROLE_COMMON · NORMAL · ACTIVE). RUN_TAG 가 실행마다 달라 이전 회원을 재사용하지 않는다.
#   테스트가 끝나면 run.sh 가 reset.sh 로 회원·예매를 함께 지운다.
#   비밀번호 해시는 BCrypt cost 4 로 실행 시 생성(htpasswd)하고 파일·로그에 남기지 않는다.
#   cost 4 → 로그인 검증이 빨라 15만 명 로그인이 access 토큰 수명(로컬 30분) 안에 끝난다.
#
# 사용법:
#   RUN_TAG=... LOAD_TEST_PASSWORD=... bash scripts/load/seat-race/seed-members.sh
#   (run.sh 가 RUN_TAG·MEMBER_COUNT 를 정해 호출한다)
#
# 환경변수 (DB_* 는 미지정 시 저장소 루트 .env 의 값):
#   RUN_TAG(필수, 소문자·숫자 1~20자) LOAD_TEST_PASSWORD(필수) MEMBER_COUNT(150000, 최대 999999)
#   DB_PASS(MYSQL_PASSWORD) DB_NAME(MYSQL_DATABASE) DB_USER(MYSQL_USER) COMPOSE_SERVICE(mysql)
#
# DB 접속 정보는 이 파일에 쓰지 않는다. .env 또는 환경변수로만 받는다.
# =============================================================================
# -e: 명령 실패 시 중단, -u: 미정의 변수 사용 시 중단, -o pipefail: 파이프 중간 실패도 실패로 처리
set -euo pipefail

# 저장소 루트로 이동(docker-compose.yml 위치)
cd "$(dirname "$0")/../../.."
# shellcheck source=../common.sh
source scripts/load/common.sh

# ----- 설정 (환경변수 + 기본값, 시크릿은 필수 입력) -----
MEMBER_COUNT="${MEMBER_COUNT:-150000}"
DB_NAME="${DB_NAME:-${MYSQL_DATABASE:?.env 에 MYSQL_DATABASE 가 없습니다(cp .env.example .env)}}"
DB_USER="${DB_USER:-${MYSQL_USER:?.env 에 MYSQL_USER 가 없습니다(cp .env.example .env)}}"
COMPOSE_SERVICE="${COMPOSE_SERVICE:-mysql}"
RUN_TAG="${RUN_TAG:?RUN_TAG 를 지정하세요(run.sh 가 실행마다 만든다)}"
DB_PASS="${DB_PASS:-${MYSQL_PASSWORD:?.env 에 MYSQL_PASSWORD 가 없습니다(cp .env.example .env)}}"
: "${LOAD_TEST_PASSWORD:?LOAD_TEST_PASSWORD 를 지정하세요}"

# ----- 입력값 형식 검증 -----
[[ "$MEMBER_COUNT" =~ ^[1-9][0-9]{0,5}$ ]] \
  || { fail "MEMBER_COUNT 는 1~999999 정수여야 합니다: $MEMBER_COUNT"; exit 1; }
# RUN_TAG 는 SQL 문자열에 들어가므로 소문자·숫자만 허용한다
[[ "$RUN_TAG" =~ ^[a-z0-9]{1,20}$ ]] || { fail "RUN_TAG 형식 오류(소문자·숫자 1~20자): $RUN_TAG"; exit 1; }
for v in DB_NAME DB_USER COMPOSE_SERVICE; do
  [[ "${!v}" =~ ^[A-Za-z0-9_-]+$ ]] || { fail "$v 형식 오류: ${!v}"; exit 1; }
done

# 이번 실행 회원의 이메일 접두어. 회원 번호는 6자리(loadtest-<RUN_TAG>-000001)
EMAIL_PREFIX="loadtest-${RUN_TAG}-"
SEQ_CTE="WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < $MEMBER_COUNT)"
NUM_EXPR="LPAD(n, 6, '0')"
RECURSION="SET SESSION cte_max_recursion_depth = $((MEMBER_COUNT + 1));"

# MySQL 컨테이너에서 stdin 의 SQL 실행(-N -B: 머리글 없는 탭 구분). 비밀번호 경고 줄은 숨긴다
mysql_exec() {
  docker compose exec -T "$COMPOSE_SERVICE" \
    mysql --default-character-set=utf8mb4 -N -B -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" \
    2> >(grep -v 'Using a password on the command line' >&2)
}

# 이번 실행 회원 현황 → 전역 ROWS(행 수) / DISTINCT(이메일 수) / ACTIVE(ACTIVE + 고객 등급 수)
# 접두어 '-' 는 LIKE 와일드카드가 아니라 그대로 비교된다(email 인덱스 범위 검색)
count_members() {
  local out
  out="$(printf "SELECT COUNT(*), COUNT(DISTINCT email), %s FROM member WHERE email LIKE '%s%%';\n" \
    "COALESCE(SUM(status = 'ACTIVE' AND grade = 'ROLE_COMMON'), 0)" "$EMAIL_PREFIX" \
    | mysql_exec)" || { fail "회원 현황 조회 실패(DB_USER·DB_PASS 확인)"; exit 1; }
  read -r ROWS DISTINCT ACTIVE <<<"$out"
  # 세 값이 모두 숫자가 아니면 조회 결과를 믿을 수 없으므로 중단
  local v
  for v in ROWS DISTINCT ACTIVE; do
    [[ "${!v:-}" =~ ^[0-9]+$ ]] || { fail "회원 현황 결과 형식 오류: ${out:-없음}"; exit 1; }
  done
}

# ----- 1. 사전 점검 -----
header "회원 적재 — 사전 점검"
require_cmd docker htpasswd
# MySQL 컨테이너가 떠 있어야 한다(자동 기동하지 않음)
RUNNING="$(docker compose ps --status running --services 2>/dev/null || true)"
grep -qx "$COMPOSE_SERVICE" <<<"$RUNNING" \
  || { fail "MySQL 컨테이너($COMPOSE_SERVICE)가 실행 중이 아닙니다: docker compose up -d"; exit 1; }
# 새로 만들 이메일이 이미 있으면(같은 RUN_TAG 재사용) 1인 1회를 보장할 수 없으므로 중단
count_members
[ "$ROWS" = "0" ] || { fail "${EMAIL_PREFIX}* 회원이 이미 ${ROWS}명 있습니다(다른 RUN_TAG 로 실행)"; exit 1; }

# ----- 2. 새 회원 INSERT -----
header "회원 적재 — INSERT (${EMAIL_PREFIX}* ${MEMBER_COUNT}명)"
# BCrypt cost 4 해시: 비밀번호는 stdin(-i)으로 넘겨 프로세스 인자에 남기지 않는다
PW_HASH="$(printf '%s\n' "$LOAD_TEST_PASSWORD" | htpasswd -inBC 4 loadtest)"
PW_HASH="${PW_HASH#*:}"
# htpasswd 는 $2y$ 를 만든다. ASCII 비밀번호에선 $2a$ 와 같아 Spring 기본 형식으로 맞춘다
PW_HASH="\$2a\$${PW_HASH#\$2?\$}"
[[ "$PW_HASH" =~ ^\$2a\$04\$[./A-Za-z0-9]{53}$ ]] || { fail "BCrypt 해시 생성 실패"; exit 1; }

started=$SECONDS
# 해시는 SQL 변수로 stdin 에만 싣는다
# 컬럼 값은 docs/test-data.sql 의 정상 일반 회원(ROLE_COMMON·NORMAL·ACTIVE)과 같은 형식
printf "%s\nSET @pw = '%s';\n%s\n%s\n%s\n%s\n%s\n" \
  "$RECURSION" "$PW_HASH" \
  "INSERT INTO member (email, username, password, phone_number, birthday, grade, social_provider,
     status, created_at, updated_at)" \
  "$SEQ_CTE" \
  "SELECT CONCAT('$EMAIL_PREFIX', $NUM_EXPR, '@example.com'), CONCAT('loadtest', $NUM_EXPR), @pw,
     CONCAT('010', LPAD(n, 8, '0')), '19900101', 'ROLE_COMMON', 'NORMAL', 'ACTIVE', NOW(6), NOW(6)" \
  "FROM seq;" \
  "SET @pw = NULL;" | mysql_exec || { unset PW_HASH; fail "회원 INSERT 실패"; exit 1; }
unset PW_HASH
info "INSERT 소요 $((SECONDS - started))초"

# ----- 3. 결과 확인: 정확히 MEMBER_COUNT 명이 중복 없이 활성이어야 한다 -----
header "회원 적재 — 결과 확인"
count_members
if [ "$ROWS" = "$MEMBER_COUNT" ] && [ "$DISTINCT" = "$MEMBER_COUNT" ] && [ "$ACTIVE" = "$MEMBER_COUNT" ]; then
  pass "회원 ${MEMBER_COUNT}명 새로 생성 완료(${EMAIL_PREFIX}000001 ~)"
  exit 0
fi
fail "회원 수 불일치: 행 ${ROWS} / 이메일 ${DISTINCT} / 활성 ${ACTIVE} (기대 ${MEMBER_COUNT})"
exit 1
