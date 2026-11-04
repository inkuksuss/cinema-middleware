#!/usr/bin/env bash
# =============================================================================
# 좌석 선점 경쟁 부하 테스트 러너 (회원 15만 · 회차 3개 x 1000석 · 초당 5000건 30초)
#
#   1. 사전 점검: k6·jq·curl·node·docker·htpasswd, 필수 환경변수, 서버 응답
#   2. 회원 적재: seed-members.sh 가 이번 실행 전용 회원 TOTAL 명을 MySQL 에 새로 INSERT
#      (이메일 loadtest-<RUN_TAG>-NNNNNN, RUN_TAG = 실행 시각이라 이전 회원을 재사용하지 않음)
#   3. 회차·토큰 준비: seed.js 가 TOTAL 명 로그인 → 토큰 파일, 상영관 SCREENINGS 개 x 좌석 SEATS 개
#      → 관마다 ON_SALE 회차 1개(상영일 2099-12-31)
#   4. 좌석 배치 API 로 회차 사전 검사(ON_SALE, 점유 0석, 판매 가능 SEATS 석) → targets.json
#   5. k6 실행(constant-arrival-rate: 초당 RATE 건 x DURATION = TOTAL 건, 회원 1명당 1회)
#   6. verify.sh 로 판정(회차별 SEATS 석 RESERVED, 예상 밖 응답 0, 응답 TOTAL 건, dropped 0)
#   7. 정리: 종료 시(실패 포함) 토큰 파일 삭제 + reset.sh 로 테스트 데이터 전부 삭제
#      (KEEP_DATA=1 이면 데이터를 남긴다)
#
# 사용법:
#   DB_PASS=... ./scripts/load/seat-race/run.sh
#   RATE=1000 DURATION=150s DB_PASS=... ./scripts/load/seat-race/run.sh   # RATE x 초 = TOTAL
#
# 환경변수:
#   DB_PASS(필수, docker-compose.yml 의 MYSQL_PASSWORD) BASE_URL(http://localhost:8080)
#   TOTAL(150000, 회원 수 = 요청 수) RATE(5000) DURATION(30s) PRE_VUS(2000) MAX_VUS(5000)
#   SCREENINGS(3) SEATS(1000) REQ_TIMEOUT(30s) GRACEFUL_STOP(60s) LOGIN_CONCURRENCY(200)
#   TOKEN_TTL_SEC(1800, 로컬 access 토큰 수명) CINEMA_ID(1) MOVIE_ID(미지정 시 자동 선택)
#   RESULT_DIR(build/load-test/seat-race/<시각>) LOAD_TEST_PASSWORD(회원 비밀번호, 기본값 있음)
#   KEEP_DATA(0, 1 이면 종료 시 데이터 정리 생략)
#
# 관리자 API 는 현재 SecurityConfig 가 permitAll 이라 토큰 없이 호출한다.
# =============================================================================
# -e: 명령 실패 시 중단, -u: 미정의 변수 사용 시 중단, -o pipefail: 파이프 중간 실패도 실패로 처리
set -euo pipefail

# 저장소 루트로 이동(build 경로·docker-compose.yml 기준)
cd "$(dirname "$0")/../../.."
# shellcheck source=../common.sh
source scripts/load/common.sh

SCENARIO_DIR="scripts/load/seat-race"

# ----- 설정 (환경변수 + 기본값) -----
BASE="${BASE_URL:-http://localhost:8080}"
TOTAL="${TOTAL:-150000}"
RATE="${RATE:-5000}"
DURATION="${DURATION:-30s}"
PRE_VUS="${PRE_VUS:-2000}"
MAX_VUS="${MAX_VUS:-5000}"
SCREENINGS="${SCREENINGS:-3}"
SEATS="${SEATS:-1000}"
REQ_TIMEOUT="${REQ_TIMEOUT:-30s}"
GRACEFUL_STOP="${GRACEFUL_STOP:-60s}"
LOGIN_CONCURRENCY="${LOGIN_CONCURRENCY:-200}"
TOKEN_TTL_SEC="${TOKEN_TTL_SEC:-1800}"
CINEMA_ID="${CINEMA_ID:-1}"
MOVIE_ID="${MOVIE_ID:-}"
RESULT_DIR="${RESULT_DIR:-build/load-test/seat-race/$(date +%Y%m%d-%H%M%S)}"
# 회원 적재(seed-members.sh)용 DB 비밀번호. 이 파일에 값을 쓰지 않고 환경변수로만 받는다.
# export 하지 않고 seed-members.sh 에만 넘긴다(node·k6 프로세스 환경에 남기지 않음)
: "${DB_PASS:?DB_PASS 를 지정하세요(docker-compose.yml 의 MYSQL_PASSWORD)}"
# 이번 실행 회원 이메일 표식(실행 시각). 실행마다 달라 회원을 새로 만든다
RUN_TAG="$(date +%y%m%d%H%M%S)"
# 1 이면 종료 시 reset.sh 를 건너뛴다(데이터 확인용)
KEEP_DATA="${KEEP_DATA:-0}"
# 테스트 회원 비밀번호. 이 파일에 한해 기본값을 둔다(사용자 허용, 로컬 테스트 데이터용 공통 비밀번호)
LOAD_TEST_PASSWORD="${LOAD_TEST_PASSWORD:-test1234!}"  # secret-ok
export LOAD_TEST_PASSWORD

# ----- 입력값 형식 검증 -----
[[ "$BASE" =~ ^https?://[A-Za-z0-9.:-]+$ ]] || { fail "BASE_URL 형식 오류: $BASE"; exit 1; }
for v in TOTAL RATE PRE_VUS MAX_VUS SCREENINGS SEATS LOGIN_CONCURRENCY TOKEN_TTL_SEC CINEMA_ID; do
  [[ "${!v}" =~ ^[1-9][0-9]*$ ]] || { fail "$v 는 양의 정수여야 합니다: ${!v}"; exit 1; }
done
[[ -z "$MOVIE_ID" || "$MOVIE_ID" =~ ^[1-9][0-9]*$ ]] || { fail "MOVIE_ID 는 양의 정수여야 합니다"; exit 1; }
for v in REQ_TIMEOUT GRACEFUL_STOP; do
  [[ "${!v}" =~ ^[0-9]+(ms|s|m)$ ]] || { fail "$v 형식 오류(예: 30s): ${!v}"; exit 1; }
done
# DURATION 은 초 단위만 받는다(RATE x 초 = TOTAL 계산용)
[[ "$DURATION" =~ ^([1-9][0-9]*)s$ ]] || { fail "DURATION 은 초 단위여야 합니다(예: 30s)"; exit 1; }
DURATION_SEC="${BASH_REMATCH[1]}"
# 회원 1명당 1회 = 시작 반복 수(RATE x 초)가 회원 수와 같아야 한다
[ "$((RATE * DURATION_SEC))" -eq "$TOTAL" ] \
  || { fail "RATE($RATE) x DURATION(${DURATION_SEC}s) = $((RATE * DURATION_SEC)) ≠ TOTAL($TOTAL)"; exit 1; }
[ "$TOTAL" -le 999999 ] || { fail "TOTAL 은 999999 이하여야 합니다(이메일 번호 6자리)"; exit 1; }
[ "$MAX_VUS" -ge "$PRE_VUS" ] || { fail "MAX_VUS 는 PRE_VUS 이상이어야 합니다"; exit 1; }
# k6 시나리오가 한 요청에 최대 4석을 고른다
[ "$SEATS" -ge 4 ] || { fail "SEATS 는 4 이상이어야 합니다"; exit 1; }

# 결과 디렉터리와 입력 파일 경로(k6 open() 은 스크립트 기준 상대 경로라 절대 경로로 넘긴다)
mkdir -p "$RESULT_DIR"
RESULT_DIR="$(cd "$RESULT_DIR" && pwd)"
TOKEN_FILE="$RESULT_DIR/tokens.json"
TARGET_FILE="$RESULT_DIR/targets.json"
SUMMARY="$RESULT_DIR/summary.json"
# ----- 7. 정리 (EXIT trap: 성공·실패·중단 모두) -----
# 토큰 파일(유효한 access 토큰 15만 개)을 지우고 reset.sh 로 테스트 데이터를 지운다.
# 정리 실패는 경고만 하고 종료 코드는 테스트 결과를 유지한다
on_exit() {
  local code=$?
  rm -f "$TOKEN_FILE"
  header "7. 정리"
  if [ "$KEEP_DATA" = "1" ]; then
    info "KEEP_DATA=1 — 데이터를 남깁니다. 정리: DB_PASS=... bash $SCENARIO_DIR/reset.sh"
  else
    DB_PASS="$DB_PASS" bash "$SCENARIO_DIR/reset.sh" \
      || warn "데이터 정리 실패 — 다시 실행: DB_PASS=... bash $SCENARIO_DIR/reset.sh"
  fi
  exit "$code"
}
trap on_exit EXIT

# ----- 1. 사전 점검 -----
preflight() {
  header "1. 사전 점검"
  require_cmd k6 jq curl node docker htpasswd
  info "$(k6 version | head -1)"
  wait_for_server "$BASE" 30
}

# ----- 2. 회원 적재 (seed-members.sh) -----
seed_members() {
  header "2. 회원 적재 (새 회원 ${TOTAL}명, RUN_TAG=${RUN_TAG})"
  DB_PASS="$DB_PASS" RUN_TAG="$RUN_TAG" MEMBER_COUNT="$TOTAL" bash "$SCENARIO_DIR/seed-members.sh" || { fail "회원 적재 실패"; exit 1; }
}

# ----- 3. 회차·토큰 준비 (seed.js) -----
# 로그는 stderr 로 그대로 보이고 stdout 은 결과 JSON 한 줄
seed_screenings() {
  header "3. 회차·토큰 준비 (회차 ${SCREENINGS}개 x ${SEATS}석, 로그인 ${TOTAL}명)"
  local out
  # 표식 상수는 common.sh 값을 넘겨 단일 소스를 유지한다. 실패해도 종료 시 reset.sh 가 정리
  out="$(BASE_URL="$BASE" RUN_TAG="$RUN_TAG" MEMBER_COUNT="$TOTAL" LOGIN_CONCURRENCY="$LOGIN_CONCURRENCY" \
    SCREENING_COUNT="$SCREENINGS" SEATS="$SEATS" CINEMA_ID="$CINEMA_ID" MOVIE_ID="$MOVIE_ID" \
    TOKEN_FILE="$TOKEN_FILE" LOADTEST_SCREENING_DATE="$LOADTEST_SCREENING_DATE" \
    LOADTEST_THEATER_PREFIX="$LOADTEST_THEATER_PREFIX" \
    node "$SCENARIO_DIR/seed.js")" || { fail "회차·토큰 준비(seed.js) 실패"; exit 1; }
  local json
  json="$(tail -n1 <<<"$out")"
  # 회차·상영관 id 는 공백 구분 목록으로, 토큰 수·로그인 시작 시각은 숫자로 꺼낸다
  SCREENING_IDS="$(jq -r '.screeningIds | map(tostring) | join(" ")' <<<"$json" 2>/dev/null || true)"
  THEATER_IDS="$(jq -r '.theaterIds | map(tostring) | join(" ")' <<<"$json" 2>/dev/null || true)"
  TOKEN_COUNT="$(jq -r '.tokenCount // empty' <<<"$json" 2>/dev/null || true)"
  TOKEN_ISSUED_AT="$(jq -r '.tokenIssuedAt // empty' <<<"$json" 2>/dev/null || true)"
  [[ "$SCREENING_IDS" =~ ^[1-9][0-9]*( [1-9][0-9]*)*$ ]] \
    || { fail "seed.js 출력에서 screeningIds 를 읽지 못했습니다: ${json:-없음}"; exit 1; }
  [ "$(wc -w <<<"$SCREENING_IDS" | tr -d ' ')" -eq "$SCREENINGS" ] \
    || { fail "회차 수가 ${SCREENINGS}개가 아닙니다: $SCREENING_IDS"; exit 1; }
  [ "${TOKEN_COUNT:-0}" = "$TOTAL" ] || { fail "토큰 수 ${TOKEN_COUNT:-?} ≠ TOTAL $TOTAL"; exit 1; }
  [[ "$TOKEN_ISSUED_AT" =~ ^[0-9]+$ ]] || { fail "seed.js 출력에 tokenIssuedAt 이 없습니다"; exit 1; }
  [ -s "$TOKEN_FILE" ] || { fail "토큰 파일이 없습니다: $TOKEN_FILE"; exit 1; }
  info "회차: $SCREENING_IDS / 상영관: $THEATER_IDS / 토큰 ${TOKEN_COUNT}개"
}

# ----- 4. 좌석 배치 API 로 회차 사전 검사 → targets.json -----
load_targets() {
  header "4. 회차 사전 검사 (좌석 배치 API)"
  local sid status reserved total available rows=()
  for sid in $SCREENING_IDS; do
    api_call GET "$BASE/api/v1/screening/$sid/seat" || exit 1
    # 회차 상태, 좌석 수, 이미 점유된 좌석 수, 판매 가능 좌석 수를 뽑는다
    status="$(jq -r '.data.screeningStatus' <<<"$API_BODY")"
    total="$(jq -r '.data.seatList | length' <<<"$API_BODY")"
    reserved="$(jq -r '[.data.seatList[] | select(.reservationSeatStatus == "RESERVED")] | length' \
      <<<"$API_BODY")"
    available="$(jq -r '[.data.seatList[] | select(.seatStatus == "AVAILABLE") | .theaterSeatId]
      | unique | length' <<<"$API_BODY")"
    [ "$status" = "ON_SALE" ] || { fail "회차 $sid 상태가 ON_SALE 이 아닙니다: $status"; exit 1; }
    # 정확히 SEATS 석이 채워지는지 보려면 빈 회차여야 한다
    [ "$reserved" = "0" ] || { fail "회차 $sid 에 이미 점유된 좌석 ${reserved}석이 있습니다"; exit 1; }
    [ "$total" = "$SEATS" ] && [ "$available" = "$SEATS" ] \
      || { fail "회차 $sid 좌석 ${total}석·판매 가능 ${available}석(기대 ${SEATS}석)"; exit 1; }
    # 판매 가능 좌석 id 배열(JSON) 을 회차 순서대로 모은다
    rows+=("$(jq -c '[.data.seatList[] | select(.seatStatus == "AVAILABLE") | .theaterSeatId] | unique' \
      <<<"$API_BODY")")
    info "회차 $sid: ON_SALE, 점유 0석, 판매 가능 ${available}석"
  done
  # {"screeningIds":[..],"seatIds":[[..],[..],..]} 형태로 k6 입력 파일을 만든다
  printf '%s\n' "${rows[@]}" | jq -cs --arg ids "$SCREENING_IDS" \
    '{screeningIds: ($ids | split(" ") | map(tonumber)), seatIds: .}' >"$TARGET_FILE"
}

# 토큰이 부하 도중 만료되지 않는지 확인: 로그인 시작 후 경과 + 부하 + 여유 60초 < 토큰 수명
check_token_age() {
  local elapsed=$(( $(date +%s) - TOKEN_ISSUED_AT / 1000 ))
  [ $((elapsed + DURATION_SEC + 60)) -lt "$TOKEN_TTL_SEC" ] \
    || { fail "토큰 발급 후 ${elapsed}초 경과 — 부하 중 만료 위험(TOKEN_TTL_SEC=$TOKEN_TTL_SEC)"; exit 1; }
  info "토큰 발급 후 ${elapsed}초 경과(수명 ${TOKEN_TTL_SEC}초)"
}

# ----- 5. k6 실행 -----
run_k6() {
  header "5. k6 실행 (초당 ${RATE}건 x ${DURATION} = ${TOTAL}건, VU ${PRE_VUS}~${MAX_VUS})"
  check_token_age
  # 토큰·좌석은 파일 경로로만 넘긴다(프로세스 인자에 토큰을 남기지 않음)
  set +e
  k6 run \
    --summary-export "$SUMMARY" \
    -e BASE_URL="$BASE" -e TOKEN_FILE="$TOKEN_FILE" -e TARGET_FILE="$TARGET_FILE" \
    -e TOTAL="$TOTAL" -e RATE="$RATE" -e DURATION="$DURATION" \
    -e PRE_VUS="$PRE_VUS" -e MAX_VUS="$MAX_VUS" \
    -e REQ_TIMEOUT="$REQ_TIMEOUT" -e GRACEFUL_STOP="$GRACEFUL_STOP" \
    "$SCENARIO_DIR/seat-race.js"
  K6_EXIT_CODE=$?
  set -e
  # 99 = 임계값 실패(판정은 verify.sh 가 다시 한다), 그 외 0 이 아니면 실행 자체 오류
  case "$K6_EXIT_CODE" in
    0) info "k6 종료 코드 0 (임계값 통과)" ;;
    99) warn "k6 종료 코드 99 (임계값 실패) — 아래 검증에서 원인 확인" ;;
    *) fail "k6 실행 오류(종료 코드 $K6_EXIT_CODE)"; [ -f "$SUMMARY" ] || exit "$K6_EXIT_CODE" ;;
  esac
}

# ----- 6. 검증 -----
verify() {
  header "6. 검증"
  set +e
  # shellcheck disable=SC2086  # 회차 id 목록을 인자 여러 개로 넘긴다
  K6_EXIT_CODE="$K6_EXIT_CODE" EXPECTED_SEATS="$SEATS" TOTAL="$TOTAL" BASE_URL="$BASE" \
    bash "$SCENARIO_DIR/verify.sh" "$SUMMARY" $SCREENING_IDS
  VERIFY_EXIT=$?
  set -e
}

preflight
seed_members
seed_screenings
load_targets
run_k6
verify
info "결과: $RESULT_DIR (summary.json, targets.json)"
exit "$VERIFY_EXIT"
