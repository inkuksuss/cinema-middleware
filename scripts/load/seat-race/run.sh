#!/usr/bin/env bash
# =============================================================================
# 좌석 선점 경쟁 부하 테스트 러너 (DB 직접 접속 없음 — 관리자 API 로 데이터 준비)
#
#   1. 사전 점검: k6·jq·curl·node, 필수 환경변수, 서버 응답
#   2. 회차 준비(SCREENING_ID 미지정 시): seed.js(Node)가 관리자 API 로
#      상영관 생성 → 좌석 EXPECTED_SEATS 개 일괄 생성 → ON_SALE 회차 생성(상영일 2099-12-31)
#   3. 좌석 배치 API 로 회차 사전 검사(ON_SALE, 점유 0석, 판매 가능 좌석 수) + 좌석 id 목록 확보
#   4. k6 실행(정확히 TOTAL 건, 초당 최대 RATE 건)
#   5. verify.sh 로 판정: 좌석 배치 API(300석 RESERVED) + 응답 집계 (PASS/FAIL)
#   6. 정리 안내(cleanup.sh — 예매가 남은 회차는 API 로 지울 수 없음, README 참고)
#
# 사용법:
#   LOAD_TEST_PASSWORD=... ./scripts/load/seat-race/run.sh
#   RATE=1000 LOAD_TEST_PASSWORD=... ./scripts/load/seat-race/run.sh   # 50초에 걸쳐 전송
#   SCREENING_ID=1051 LOAD_TEST_PASSWORD=... ./scripts/load/seat-race/run.sh  # 기존 빈 회차
#
# 환경변수:
#   BASE_URL(http://localhost:8080) TOTAL(50000) RATE(5000) VUS(5000) MAX_DURATION(10m)
#   USER_COUNT(100) USER_START(5) REQ_TIMEOUT(30s) EXPECTED_SEATS(300, 새 회차는 400 이하)
#   SCREENING_ID(미지정 시 새로 생성) CINEMA_ID(1) MOVIE_ID(미지정 시 DISPLAY 영화 자동 선택)
#   RESULT_DIR(build/load-test/seat-race/<시각>) LOAD_TEST_PASSWORD(테스트 회원 비밀번호, 기본값 있음)
#
# 관리자 API 는 현재 SecurityConfig 가 permitAll 이라 토큰 없이 호출한다.
# =============================================================================
# -e: 명령 실패 시 중단, -u: 미정의 변수 사용 시 중단, -o pipefail: 파이프 중간 실패도 실패로 처리
set -euo pipefail

# 저장소 루트로 이동(build 경로 기준)
cd "$(dirname "$0")/../../.."
# shellcheck source=../common.sh
source scripts/load/common.sh

SCENARIO_DIR="scripts/load/seat-race"

# ----- 설정 (환경변수 + 기본값) -----
BASE="${BASE_URL:-http://localhost:8080}"
TOTAL="${TOTAL:-50000}"
RATE="${RATE:-5000}"
VUS="${VUS:-5000}"
MAX_DURATION="${MAX_DURATION:-10m}"
USER_COUNT="${USER_COUNT:-100}"
USER_START="${USER_START:-5}"
REQ_TIMEOUT="${REQ_TIMEOUT:-30s}"
EXPECTED_SEATS="${EXPECTED_SEATS:-300}"
SCREENING_ID="${SCREENING_ID:-}"
CINEMA_ID="${CINEMA_ID:-1}"
MOVIE_ID="${MOVIE_ID:-}"
RESULT_DIR="${RESULT_DIR:-build/load-test/seat-race/$(date +%Y%m%d-%H%M%S)}"
# 테스트 회원 비밀번호. 이 파일에 한해 기본값을 둔다(사용자 허용, 로컬 테스트 데이터용 공통 비밀번호)
LOAD_TEST_PASSWORD="${LOAD_TEST_PASSWORD:-test1234!}"  # secret-ok
export LOAD_TEST_PASSWORD

# ----- 입력값 형식 검증 -----
[[ "$BASE" =~ ^https?://[A-Za-z0-9.:-]+$ ]] || { fail "BASE_URL 형식 오류: $BASE"; exit 1; }
for v in TOTAL RATE VUS USER_COUNT USER_START EXPECTED_SEATS CINEMA_ID; do
  [[ "${!v}" =~ ^[1-9][0-9]*$ ]] || { fail "$v 는 양의 정수여야 합니다: ${!v}"; exit 1; }
done
for v in MAX_DURATION REQ_TIMEOUT; do
  [[ "${!v}" =~ ^[0-9]+(ms|s|m)$ ]] || { fail "$v 형식 오류(예: 10s): ${!v}"; exit 1; }
done
for v in SCREENING_ID MOVIE_ID; do
  [[ -z "${!v}" || "${!v}" =~ ^[1-9][0-9]*$ ]] || { fail "$v 는 양의 정수여야 합니다"; exit 1; }
done
# VU 마다 같은 횟수를 보내야 총 요청 수가 정확히 TOTAL 이 된다
[ "$((TOTAL % VUS))" -eq 0 ] || { fail "TOTAL($TOTAL) 이 VUS($VUS) 로 나누어떨어져야 합니다"; exit 1; }
# 좌석 일괄 생성 API 는 한 번에 최대 400개(TheaterRequest.BulkCreateSeat @Size)
if [ -z "$SCREENING_ID" ] && [ "$EXPECTED_SEATS" -gt 400 ]; then
  fail "새 회차의 EXPECTED_SEATS 는 400 이하여야 합니다(좌석 일괄 생성 API 제한)"
  exit 1
fi

# ----- 1. 사전 점검 -----
preflight() {
  header "1. 사전 점검"
  require_cmd k6 jq curl node
  info "$(k6 version | head -1)"
  wait_for_server "$BASE" 30
}

# ----- 2. 회차 준비 (seed.js) -----
# 새 회차는 seed.js 가 관리자 API 로 만든다. 로그는 stderr 로 그대로 보이고 stdout 은 JSON 한 줄
create_screening() {
  local out
  # 표식 상수는 common.sh 값을 넘겨 단일 소스를 유지한다. 실패 시 seed.js 가 정리 명령을 안내
  out="$(BASE_URL="$BASE" CINEMA_ID="$CINEMA_ID" MOVIE_ID="$MOVIE_ID" \
    EXPECTED_SEATS="$EXPECTED_SEATS" LOADTEST_SCREENING_DATE="$LOADTEST_SCREENING_DATE" \
    LOADTEST_THEATER_PREFIX="$LOADTEST_THEATER_PREFIX" \
    node "$SCENARIO_DIR/seed.js")" || { fail "회차 준비(seed.js) 실패"; exit 1; }
  # 마지막 줄 JSON 에서 회차 id 를 꺼내 형식을 확인한다
  SCREENING_ID="$(tail -n1 <<<"$out" | jq -r '.screeningId // empty' 2>/dev/null || true)"
  [[ "$SCREENING_ID" =~ ^[1-9][0-9]*$ ]] \
    || { fail "seed.js 출력에서 screeningId 를 읽지 못했습니다: ${out:-없음}"; exit 1; }
  info "준비된 회차: screening_id=$SCREENING_ID"
}

prepare_screening() {
  header "2. 회차 준비"
  if [ -z "$SCREENING_ID" ]; then
    create_screening
  else
    info "지정 회차 사용: $SCREENING_ID (새로 만들지 않음)"
  fi
}

# ----- 3. 좌석 배치 API 로 사전 검사 + 좌석 목록 -----
load_seats() {
  header "3. 회차 사전 검사 (좌석 배치 API)"
  api_call GET "$BASE/api/v1/screening/$SCREENING_ID/seat" || exit 1
  local status reserved count
  # 회차 상태, 이미 점유된 좌석 수, 판매 가능 좌석 id 목록을 뽑는다
  status="$(jq -r '.data.screeningStatus' <<<"$API_BODY")"
  reserved="$(jq -r '[.data.seatList[] | select(.reservationSeatStatus == "RESERVED")] | length' \
    <<<"$API_BODY")"
  SEAT_IDS="$(jq -r '[.data.seatList[] | select(.seatStatus == "AVAILABLE") | .theaterSeatId]
    | unique | map(tostring) | join(",")' <<<"$API_BODY")"
  [ "$status" = "ON_SALE" ] || { fail "회차 $SCREENING_ID 상태가 ON_SALE 이 아닙니다: $status"; exit 1; }
  # 300석이 정확히 채워지는지 보려면 빈 회차여야 한다
  [ "$reserved" = "0" ] || { fail "회차 $SCREENING_ID 에 이미 점유된 좌석 ${reserved}석이 있습니다"; exit 1; }
  [[ "$SEAT_IDS" =~ ^[0-9]+(,[0-9]+)*$ ]] || { fail "판매 가능 좌석이 없습니다"; exit 1; }
  count="$(awk -F, '{print NF}' <<<"$SEAT_IDS")"
  # 판매 가능 좌석 수가 기대값과 다르면 기준 1(정확히 300석)을 판정할 수 없으므로 중단
  [ "$count" -eq "$EXPECTED_SEATS" ] \
    || { fail "판매 가능 좌석이 ${count}석입니다(기대 ${EXPECTED_SEATS}석)"; exit 1; }
  info "회차 $SCREENING_ID: ON_SALE, 점유 0석, 판매 가능 ${count}석"
}

# ----- 4. k6 실행 -----
run_k6() {
  header "4. k6 실행 (총 ${TOTAL}건, 초당 최대 ${RATE}건, VU ${VUS})"
  mkdir -p "$RESULT_DIR"
  SUMMARY="$RESULT_DIR/summary.json"
  # 비밀번호는 -e 로 넘기지 않고 환경변수(export)로 전달해 프로세스 인자에 남기지 않는다
  set +e
  k6 run \
    --summary-export "$SUMMARY" \
    -e BASE_URL="$BASE" -e SCREENING_ID="$SCREENING_ID" -e SEAT_IDS="$SEAT_IDS" \
    -e TOTAL="$TOTAL" -e RATE="$RATE" -e VUS="$VUS" -e MAX_DURATION="$MAX_DURATION" \
    -e REQ_TIMEOUT="$REQ_TIMEOUT" \
    -e USER_COUNT="$USER_COUNT" -e USER_START="$USER_START" \
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

# ----- 5. 검증 -----
verify() {
  header "5. 검증"
  set +e
  K6_EXIT_CODE="$K6_EXIT_CODE" EXPECTED_SEATS="$EXPECTED_SEATS" TOTAL="$TOTAL" BASE_URL="$BASE" \
    bash "$SCENARIO_DIR/verify.sh" "$SCREENING_ID" "$SUMMARY"
  VERIFY_EXIT=$?
  set -e
}

# ----- 6. 정리 안내 -----
# 예매(PENDING)가 남은 회차는 삭제 API 가 거부하므로 자동 정리하지 않는다(README 참고)
cleanup_hint() {
  header "6. 정리"
  info "데이터를 남겨 두었습니다(상영일 ${LOADTEST_SCREENING_DATE} 표식)."
  info "예매가 없는 회차만 정리 가능: bash $SCENARIO_DIR/cleanup.sh $SCREENING_ID"
}

preflight
prepare_screening
load_seats
run_k6
verify
cleanup_hint
info "결과: $RESULT_DIR (summary.json)"
exit "$VERIFY_EXIT"
