#!/usr/bin/env bash
# =============================================================================
# 좌석 선점 경쟁 테스트 데이터 정리 — run.sh 가 관리자 API 로 만든 회차·좌석·상영관을
# 관리자 DELETE API 로 지운다(DB 직접 접속 없음, 앱 동작대로 soft delete: status='DELETE')
#
# 사용법:
#   bash scripts/load/seat-race/cleanup.sh <screening_id>          # 회차 id 로(상영관까지)
#   bash scripts/load/seat-race/cleanup.sh --theater <theater_id>  # 준비 중 중단돼 관만 남았을 때
#
# 대상 조건: 상영관 이름이 'LOADTEST-seat-race-' 로 시작하고 회차 상영일이 2099-12-31
#   (common.sh 의 표식). 표식이 아닌 회차·상영관은 지우지 않는다.
# 순서: 표식 상영일 회차 DELETE → 좌석 DELETE(전부) → 상영관 DELETE
# 한계: 예매(PENDING/SUCCESS)가 남은 회차는 삭제 API 가 code 17 로 거부한다.
#   관리자 예매 취소 API 가 미구현이라, 부하를 돌린 회차는 이 스크립트로 지울 수 없다.
#   (표식 상영일이라 실제 데이터와 겹치지 않으므로 남겨 두거나 테스트 데이터를 재적재한다)
#
# 환경변수: BASE_URL(http://localhost:8080) CINEMA_ID(1, --theater 일 때 상영관 조회용)
# =============================================================================
# -e: 명령 실패 시 중단, -u: 미정의 변수 사용 시 중단, -o pipefail: 파이프 중간 실패도 실패로 처리
set -euo pipefail

# 저장소 루트로 이동
cd "$(dirname "$0")/../../.."
# shellcheck source=../common.sh
source scripts/load/common.sh

# ----- 인자·설정 검증 -----
BASE="${BASE_URL:-http://localhost:8080}"
CINEMA_ID="${CINEMA_ID:-1}"
ADMIN="$BASE/api/v1/admin"
[[ "$BASE" =~ ^https?://[A-Za-z0-9.:-]+$ ]] || { fail "BASE_URL 형식 오류: $BASE"; exit 1; }
[[ "$CINEMA_ID" =~ ^[1-9][0-9]*$ ]] || { fail "CINEMA_ID 는 양의 정수여야 합니다"; exit 1; }
usage() { fail "사용법: cleanup.sh <screening_id> | --theater <theater_id>"; exit 1; }
require_cmd curl jq

# ----- 대상 상영관 확인 (id·이름) -----
header "정리 대상 확인"
if [ "${1:-}" = "--theater" ]; then
  [[ "${2:-}" =~ ^[1-9][0-9]*$ ]] || usage
  THEATER_ID="$2"
  # 상영관 단건 조회 API 가 없어 영화관의 상영관 목록에서 id 로 이름을 찾는다
  api_call GET "$ADMIN/theater/?cinemaId=$CINEMA_ID&size=200" || exit 1
  THEATER_NAME="$(jq -r --argjson t "$THEATER_ID" \
    '[.data.content[] | select(.id == $t) | .name] | first // empty' <<<"$API_BODY")"
  [ -n "$THEATER_NAME" ] || { fail "영화관 $CINEMA_ID 에 상영관 $THEATER_ID 이 없습니다"; exit 1; }
elif [[ "${1:-}" =~ ^[1-9][0-9]*$ ]]; then
  # 회차 상세에서 상영관 id·이름과 상영일을 얻는다
  api_call GET "$ADMIN/screening/$1" || exit 1
  read -r THEATER_ID START_DATE <<<"$(jq -r '[.data.theaterId, .data.startDate] | @tsv' <<<"$API_BODY")"
  THEATER_NAME="$(jq -r '.data.theaterName' <<<"$API_BODY")"
  [ "$START_DATE" = "$LOADTEST_SCREENING_DATE" ] \
    || { fail "회차 $1 상영일이 표식($LOADTEST_SCREENING_DATE)이 아닙니다: $START_DATE"; exit 1; }
else
  usage
fi
# 표식 접두어가 없는 상영관은 테스트 밖 데이터일 수 있어 건드리지 않는다
[[ "$THEATER_NAME" == "$LOADTEST_THEATER_PREFIX"* ]] \
  || { fail "상영관 이름에 표식 접두어가 없습니다: $THEATER_NAME"; exit 1; }
info "대상 상영관: theater_id=$THEATER_ID ($THEATER_NAME)"

# ----- 1. 표식 상영일 회차 삭제 -----
header "회차 삭제"
api_call GET "$ADMIN/screening/?targetDate=$LOADTEST_SCREENING_DATE&theaterId=$THEATER_ID&size=50" \
  || exit 1
for sid in $(jq -r '.data.content[].id' <<<"$API_BODY"); do
  if ! api_call DELETE "$ADMIN/screening/$sid"; then
    # 17 = SCREENING_CANT_UPDATE: 살아 있는 예매가 있어 삭제 거부
    [ "$API_CODE" != "17" ] \
      || fail "회차 $sid 에 예매가 남아 API 로 지울 수 없습니다(관리자 예매 취소 API 미구현)"
    exit 1
  fi
  info "회차 $sid 삭제"
done

# ----- 2. 좌석 삭제 -----
header "좌석 삭제"
# 좌석이 하나도 없으면 조회 API 가 NOT_FOUND(11) 를 돌려준다 → 지울 좌석 없음
if api_call GET "$ADMIN/theater/$THEATER_ID/seat" 2>/dev/null; then
  SEATS="$(jq -r '.data.seatList[].theaterSeatId' <<<"$API_BODY")"
  n=0
  for seat in $SEATS; do
    api_call DELETE "$ADMIN/theater/$seat/seat" || exit 1
    n=$((n + 1))
  done
  info "좌석 ${n}개 삭제"
elif [ "$API_CODE" = "11" ]; then
  info "삭제할 좌석 없음"
else
  fail "좌석 목록 조회 실패"
  exit 1
fi

# ----- 3. 상영관 삭제 -----
header "상영관 삭제"
api_call DELETE "$ADMIN/theater/$THEATER_ID" || exit 1
pass "정리 완료: 상영관 $THEATER_ID 와 그 좌석·표식 회차"
