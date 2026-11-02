#!/usr/bin/env bash
# =============================================================================
# 좌석 선점 경쟁 결과 검증 (좌석 배치 API + k6 요약 집계) → PASS/FAIL
#
# 사용법:
#   bash scripts/load/seat-race/verify.sh <screening_id> <k6 summary.json>
#   (run.sh 가 k6 실행 후 자동 호출한다)
#
# 판정 (하나라도 FAIL 이면 종료 코드 1)
#   기준 1  API: GET /api/v1/screening/{id}/seat (인증 불필요, SecurityConfig permitAll)
#           응답 code 200, data.seatList 가 EXPECTED_SEATS(300)개, 그 전부 reservationSeatStatus
#           == "RESERVED" (점유 안 된 좌석은 null). 이중 점유가 있으면 행이 늘어 개수에서 걸린다
#   기준 2  k6: 성공(200)·SEAT_ALREADY_TAKEN(13) 외 결과(reserve_unexpected) = 0
#   요청 수 k6: 응답까지 끝난 요청(성공+13+그 외) = TOTAL(50000). 모자라면 FAIL
#           (MAX_DURATION 초과 등으로 중단된 요청이 있다는 뜻 — 참여자 수 고정 조건 위반)
#   참고    (판정 제외) 성공 좌석 합 vs API RESERVED 수 (DB 에는 접속하지 않는다)
#
# 환경변수: BASE_URL(http://localhost:8080) EXPECTED_SEATS(300) TOTAL(50000) K6_EXIT_CODE
# =============================================================================
# -e: 명령 실패 시 중단, -u: 미정의 변수 사용 시 중단, -o pipefail: 파이프 중간 실패도 실패로 처리
set -euo pipefail

# 저장소 루트로 이동
cd "$(dirname "$0")/../../.."
# shellcheck source=../common.sh
source scripts/load/common.sh

# ----- 인자·설정 검증 -----
SCREENING_ID="${1:-}"
SUMMARY="${2:-}"
BASE="${BASE_URL:-http://localhost:8080}"
EXPECTED_SEATS="${EXPECTED_SEATS:-300}"
TOTAL="${TOTAL:-50000}"
K6_EXIT_CODE="${K6_EXIT_CODE:-}"
[[ "$SCREENING_ID" =~ ^[1-9][0-9]*$ ]] || { fail "사용법: verify.sh <screening_id> <summary.json>"; exit 1; }
[ -f "$SUMMARY" ] || { fail "k6 요약 파일이 없습니다: ${SUMMARY:-(미지정)}"; exit 1; }
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

# ----- 기준 1: 좌석 배치 API (부하 종료 후 1회 호출) -----
header "기준 1 — 좌석 배치 API (screening_id=$SCREENING_ID)"
SEAT_URL="$BASE/api/v1/screening/$SCREENING_ID/seat"
# 본문과 HTTP 상태를 함께 받는다(마지막 줄이 상태 코드)
RAW="$(curl -sS --max-time 30 -w '\n%{http_code}' "$SEAT_URL" || true)"
HTTP_STATUS="$(tail -n1 <<<"$RAW")"
BODY="$(sed '$d' <<<"$RAW")"
info "GET $SEAT_URL → HTTP $HTTP_STATUS"

if [ "$HTTP_STATUS" != "200" ] || ! jq -e . >/dev/null 2>&1 <<<"$BODY"; then
  fail "좌석 배치 API 호출 실패(HTTP $HTTP_STATUS 또는 JSON 아님)"
  FAILED=1
  API_RESERVED=""
else
  # 응답 code, 좌석 수, RESERVED 좌석 수, 좌석 id 중복 수를 한 번에 뽑는다
  read -r API_CODE API_SEATS API_RESERVED API_DUP_IDS <<<"$(jq -r '
    (.data.seatList // []) as $seats
    | [ .code,
        ($seats | length),
        ([$seats[] | select(.reservationSeatStatus == "RESERVED")] | length),
        (($seats | length) - ([$seats[].theaterSeatId] | unique | length))
      ] | @tsv' <<<"$BODY")"
  # 성공 code 는 HttpStatus.OK(200) (ResponseCode.SUCCESS(0) 는 응답에 쓰이지 않음)
  check_eq "응답 code" "$API_CODE" 200
  check_eq "seatList 좌석 수" "$API_SEATS" "$EXPECTED_SEATS"
  check_eq "reservationSeatStatus=RESERVED 좌석 수" "$API_RESERVED" "$EXPECTED_SEATS"
  # 같은 좌석이 여러 번 나오면 이중 점유(같은 좌석에 RESERVED 행 2개 이상) 신호 — 참고 출력
  [ "$API_DUP_IDS" = "0" ] || warn "seatList 에 중복 theaterSeatId ${API_DUP_IDS}건(이중 점유 의심)"
  info "회차 상태: $(jq -r '.data.screeningStatus' <<<"$BODY")"
fi

# ----- k6 요약 집계 -----
# 요약 JSON 형식이 k6 버전마다 달라(v2 새 형식 대비) 지표 이름으로 찾은 뒤 하위의 count 를 읽는다
metric_count() {
  jq -r --arg m "$1" '
    [ (.. | objects | select(has($m)) | .[$m] | .. | objects | .count? | numbers),
      (.. | objects | select(.name? == $m) | .. | objects | .count? | numbers) ]
    | first // empty' "$SUMMARY"
}

header "기준 2·요청 수 — k6 응답 집계 ($SUMMARY)"
S="$(metric_count reserve_success)"
T="$(metric_count reserve_seat_taken)"
U="$(metric_count reserve_unexpected)"
W="$(metric_count reserve_seats_won)"
SENT="$(metric_count reserve_requests)"
# 성공·13 은 0건이면 요약에 없을 수 있어 빈 값을 0 으로 본다
S="${S:-0}"; T="${T:-0}"; W="${W:-0}"
info "성공(0) ${S}건 / SEAT_ALREADY_TAKEN(13) ${T}건 / 그 외 ${U:-?}건 / 보낸 요청 ${SENT:-?}건"

# 기준 2: reserve_unexpected 는 매 요청 0/1 을 기록하므로 요약에 반드시 있어야 한다
if [ -z "${U:-}" ]; then
  fail "기준 2 — reserve_unexpected 지표를 요약에서 찾지 못했습니다(k6 요약 출력 확인 필요)"
  FAILED=1
  U=0
else
  check_eq "기준 2 — 성공·SEAT_ALREADY_TAKEN 외 응답" "$U" 0
  # 예상 밖 code 별 내역(값이 있는 지표만)
  jq -r '[.. | objects | to_entries[] | select(.key | test("^reserve_(code_|http_error|parse_error)"))
          | .key] | unique | .[]' "$SUMMARY" | while read -r name; do
    warn "  $name = $(metric_count "$name")"
  done
fi

# 요청 수: 응답까지 분류된 요청이 정확히 TOTAL 이어야 한다
check_eq "요청 수 — 응답 완료(성공+13+그 외)" "$((S + T + U))" "$TOTAL"
[ -z "${SENT:-}" ] || [ "$SENT" = "$((S + T + U))" ] \
  || warn "보낸 요청 ${SENT} ≠ 응답 완료 $((S + T + U)) (응답 전에 중단된 요청 있음)"

# ----- 참고 (판정 제외) -----
header "참고 (판정 제외)"
# 응답 기준 성공 좌석 합과 API 의 RESERVED 수 비교(클라이언트 타임아웃 후 서버 커밋 등 탐지)
if [ -n "${API_RESERVED:-}" ] && [ "$W" != "$API_RESERVED" ]; then
  warn "성공 응답 좌석 합 ${W} ≠ API RESERVED ${API_RESERVED}"
else
  info "성공 응답 좌석 합: ${W}"
fi

# k6 임계값 결과(참고). 99 = 임계값 실패
[ -z "$K6_EXIT_CODE" ] || info "k6 종료 코드: $K6_EXIT_CODE"

# ----- 최종 판정 -----
header "최종 판정"
if [ "$FAILED" = "0" ]; then
  pass "${TOTAL}건 중 좌석 ${EXPECTED_SEATS}석 정확히 선점, 나머지는 모두 SEAT_ALREADY_TAKEN"
  exit 0
fi
fail "기준 미달 항목이 있습니다(위 FAIL 참고)"
exit 1
