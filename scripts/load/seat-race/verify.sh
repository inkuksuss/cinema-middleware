#!/usr/bin/env bash
# =============================================================================
# 좌석 선점 경쟁 결과 검증 (좌석 배치 API + k6 요약 집계) → PASS/FAIL
#
# 사용법:
#   bash scripts/load/seat-race/verify.sh <k6 summary.json> <screening_id> [<screening_id>...]
#   (run.sh 가 k6 실행 후 자동 호출한다)
#
# 판정 (하나라도 FAIL 이면 종료 코드 1)
#   기준 1  API: 회차마다 GET /api/v1/screening/{id}/seat (인증 불필요, permitAll)
#           응답 code 200, data.seatList 가 EXPECTED_SEATS(1000)개, 그 전부 RESERVED
#           (점유 안 된 좌석은 null). 이중 점유가 있으면 행이 늘어 개수에서 걸린다
#   기준 2  k6: 성공(200)·SEAT_ALREADY_TAKEN(13)·SERVICE_UNAVAILABLE(28) 외 결과 = 0
#   기준 3  k6: 응답까지 끝난 요청(성공+13+28+그 외) = TOTAL(150000)
#   기준 4  k6: dropped_iterations = 0 (VU 부족으로 시작 못 한 반복이 없어야 함. 요약에 없으면 0)
#   참고    (판정 제외) code 28 건수, 응답시간 p95/p99, 성공 좌석 합 vs API RESERVED 합
#
# 환경변수: BASE_URL(http://localhost:8080) EXPECTED_SEATS(1000) TOTAL(150000) K6_EXIT_CODE
# =============================================================================
# -e: 명령 실패 시 중단, -u: 미정의 변수 사용 시 중단, -o pipefail: 파이프 중간 실패도 실패로 처리
set -euo pipefail

# 저장소 루트로 이동
cd "$(dirname "$0")/../../.."
# shellcheck source=../common.sh
source scripts/load/common.sh

# ----- 인자·설정 검증 -----
usage() { fail "사용법: verify.sh <summary.json> <screening_id> [<screening_id>...]"; exit 1; }
SUMMARY="${1:-}"
[ "$#" -ge 2 ] || usage
shift
SCREENING_IDS=("$@")
BASE="${BASE_URL:-http://localhost:8080}"
EXPECTED_SEATS="${EXPECTED_SEATS:-1000}"
TOTAL="${TOTAL:-150000}"
K6_EXIT_CODE="${K6_EXIT_CODE:-}"
[ -f "$SUMMARY" ] || { fail "k6 요약 파일이 없습니다: ${SUMMARY:-(미지정)}"; exit 1; }
for sid in "${SCREENING_IDS[@]}"; do
  [[ "$sid" =~ ^[1-9][0-9]*$ ]] || usage
done
[[ "$BASE" =~ ^https?://[A-Za-z0-9.:-]+$ ]] || { fail "BASE_URL 형식 오류: $BASE"; exit 1; }
for v in EXPECTED_SEATS TOTAL; do
  [[ "${!v}" =~ ^[1-9][0-9]*$ ]] || { fail "$v 는 양의 정수여야 합니다"; exit 1; }
done
require_cmd curl jq

FAILED=0
# 판정 한 건 기록: check_eq <이름> <실제> <기대>
check_eq() {
  if [ "$2" = "$3" ]; then pass "$1: $2"; else fail "$1: $2 (기대 $3)"; FAILED=1; fi
}

# ----- 기준 1: 회차마다 좌석 배치 API (부하 종료 후 1회씩 호출) -----
RESERVED_SUM=0
API_OK=1
for sid in "${SCREENING_IDS[@]}"; do
  header "기준 1 — 좌석 배치 API (screening_id=$sid)"
  SEAT_URL="$BASE/api/v1/screening/$sid/seat"
  # 본문과 HTTP 상태를 함께 받는다(마지막 줄이 상태 코드)
  RAW="$(curl -sS --max-time 30 -w '\n%{http_code}' "$SEAT_URL" || true)"
  HTTP_STATUS="$(tail -n1 <<<"$RAW")"
  BODY="$(sed '$d' <<<"$RAW")"
  info "GET $SEAT_URL → HTTP $HTTP_STATUS"

  if [ "$HTTP_STATUS" != "200" ] || ! jq -e . >/dev/null 2>&1 <<<"$BODY"; then
    fail "좌석 배치 API 호출 실패(HTTP $HTTP_STATUS 또는 JSON 아님)"
    FAILED=1
    API_OK=0
    continue
  fi
  # 응답 code, 좌석 수, RESERVED 좌석 수, 좌석 id 중복 수를 한 번에 뽑는다
  read -r R_CODE R_SEATS R_RESERVED R_DUP <<<"$(jq -r '
    (.data.seatList // []) as $seats
    | [ .code,
        ($seats | length),
        ([$seats[] | select(.reservationSeatStatus == "RESERVED")] | length),
        (($seats | length) - ([$seats[].theaterSeatId] | unique | length))
      ] | @tsv' <<<"$BODY")"
  # 성공 code 는 HttpStatus.OK(200) (ResponseCode.SUCCESS(0) 는 응답에 쓰이지 않음)
  check_eq "응답 code" "$R_CODE" 200
  check_eq "seatList 좌석 수" "$R_SEATS" "$EXPECTED_SEATS"
  check_eq "reservationSeatStatus=RESERVED 좌석 수" "$R_RESERVED" "$EXPECTED_SEATS"
  # 같은 좌석이 여러 번 나오면 이중 점유(같은 좌석에 RESERVED 행 2개 이상) 신호 — 참고 출력
  [ "$R_DUP" = "0" ] || warn "seatList 에 중복 theaterSeatId ${R_DUP}건(이중 점유 의심)"
  RESERVED_SUM=$((RESERVED_SUM + R_RESERVED))
done

# ----- k6 요약 집계 -----
# 요약 JSON 형식이 k6 버전마다 달라 지표 이름으로 찾은 뒤 하위의 값(count, p(95) 등)을 읽는다
metric_stat() {
  jq -r --arg m "$1" --arg k "$2" '
    [ (.. | objects | select(has($m)) | .[$m] | .. | objects | .[$k]? | numbers),
      (.. | objects | select(.name? == $m) | .. | objects | .[$k]? | numbers) ]
    | first // empty' "$SUMMARY"
}
metric_count() { metric_stat "$1" count; }

header "기준 2~4 — k6 응답 집계 ($SUMMARY)"
S="$(metric_count reserve_success)"
T="$(metric_count reserve_seat_taken)"
B="$(metric_count reserve_busy)"
U="$(metric_count reserve_unexpected)"
W="$(metric_count reserve_seats_won)"
SENT="$(metric_count reserve_requests)"
DROPPED="$(metric_count dropped_iterations)"
# 0건인 지표는 요약에 없을 수 있어 빈 값을 0 으로 본다(reserve_unexpected 는 아래에서 따로 확인)
S="${S:-0}"; T="${T:-0}"; B="${B:-0}"; W="${W:-0}"; DROPPED="${DROPPED:-0}"
info "성공(200) ${S} / SEAT_ALREADY_TAKEN(13) ${T} / SERVICE_UNAVAILABLE(28) ${B}" \
  "/ 그 외 ${U:-?} / 보낸 요청 ${SENT:-?}"

# 기준 2: reserve_unexpected 는 매 요청 0/1 을 기록하므로 요약에 반드시 있어야 한다
if [ -z "${U:-}" ]; then
  fail "기준 2 — reserve_unexpected 지표를 요약에서 찾지 못했습니다(k6 요약 출력 확인 필요)"
  FAILED=1
  U=0
else
  check_eq "기준 2 — 성공·13·28 외 응답" "$U" 0
  # 예상 밖 결과 내역(값이 있는 지표만)
  jq -r '[.. | objects | to_entries[]
          | select(.key | test("^reserve_(code_|http_error|parse_error|no_token)")) | .key]
         | unique | .[]' "$SUMMARY" | while read -r name; do
    warn "  $name = $(metric_count "$name")"
  done
fi

# 기준 3: 응답까지 분류된 요청이 정확히 TOTAL 이어야 한다
check_eq "기준 3 — 응답 완료(성공+13+28+그 외)" "$((S + T + B + U))" "$TOTAL"
[ -z "${SENT:-}" ] || [ "$SENT" = "$((S + T + B + U))" ] \
  || warn "보낸 요청 ${SENT} ≠ 응답 완료 $((S + T + B + U)) (응답 전에 중단된 요청 있음)"

# 기준 4: VU 부족으로 시작하지 못한 반복(해당 회원은 시도 기회를 잃음)
check_eq "기준 4 — dropped_iterations" "$DROPPED" 0

# ----- 참고 (판정 제외) -----
header "참고 (판정 제외)"
info "SERVICE_UNAVAILABLE(28, 혼잡) ${B}건"
# 예매 요청 응답시간(ms): 전체·성공·13·28
for m in reserve_duration reserve_duration_success reserve_duration_seat_taken reserve_duration_busy; do
  P95="$(metric_stat "$m" 'p(95)')"
  P99="$(metric_stat "$m" 'p(99)')"
  [ -z "$P95" ] || info "$m p95 ${P95}ms / p99 ${P99:-?}ms"
done
# 응답 기준 성공 좌석 합과 API 의 RESERVED 합 비교(클라이언트 타임아웃 후 서버 커밋 등 탐지)
if [ "$API_OK" = "1" ] && [ "$W" != "$RESERVED_SUM" ]; then
  warn "성공 응답 좌석 합 ${W} ≠ API RESERVED 합 ${RESERVED_SUM}"
else
  info "성공 응답 좌석 합: ${W}"
fi
# k6 임계값 결과(참고). 99 = 임계값 실패
[ -z "$K6_EXIT_CODE" ] || info "k6 종료 코드: $K6_EXIT_CODE"

# ----- 최종 판정 -----
header "최종 판정"
if [ "$FAILED" = "0" ]; then
  pass "${TOTAL}건 중 회차 ${#SCREENING_IDS[@]}개 x ${EXPECTED_SEATS}석 정확히 선점, 나머지는 13·28 응답"
  exit 0
fi
fail "기준 미달 항목이 있습니다(위 FAIL 참고)"
exit 1
