#!/usr/bin/env bash
# =============================================================================
# 부하 테스트 공통 함수 (source 전용 — 직접 실행하지 않는다)
#
#   사용 예: source "$(dirname "$0")/../common.sh"
#
#   제공 함수
#     header / info / warn / pass / fail   : 단계 헤더·색상 메시지 출력
#     require_cmd <명령...>                  : 필요한 명령 설치 여부 점검
#     api_call <METHOD> <URL> [JSON]         : API 호출, HTTP 200 + code 200 이면 성공
#     wait_for_server <BASE_URL> [초]        : 서버가 HTTP 응답할 때까지 대기
#
#   DB 에 직접 접속하지 않는다. 테스트 데이터 정리는 각 시나리오의 reset.sh 가 한다.
# =============================================================================

# 부하 테스트가 만든 데이터의 표식: 실제 데이터와 겹치지 않는 상영일 + 상영관 이름 접두어
# (seat-race/reset.sh 는 이름 접두어가 맞는 상영관과 그 회차만 지운다)
# shellcheck disable=SC2034
LOADTEST_SCREENING_DATE='2099-12-31'
# shellcheck disable=SC2034
LOADTEST_THEATER_PREFIX='LOADTEST-seat-race-'

# 단계 헤더(청록 굵게)
header() { printf '\n\033[1;36m===== %s =====\033[0m\n' "$*"; }
# 일반 안내(파랑)
info() { printf '\033[1;34m[info]\033[0m %s\n' "$*"; }
# 경고(노랑) — 판정에는 영향 없음
warn() { printf '\033[1;33m[WARN]\033[0m %s\n' "$*"; }
# 판정 통과(초록)
pass() { printf '\033[1;32m[PASS]\033[0m %s\n' "$*"; }
# 판정 실패(빨강, stderr)
fail() { printf '\033[1;31m[FAIL]\033[0m %s\n' "$*" >&2; }

# 인자로 받은 명령이 모두 설치돼 있는지 확인하고 없으면 종료
require_cmd() {
  local c
  for c in "$@"; do
    command -v "$c" >/dev/null 2>&1 || { fail "'$c' 가 설치돼 있지 않습니다."; exit 1; }
  done
}

# API 호출: HTTP 200 + Result.code 200(성공) 이면 0, 아니면 사유 출력 후 1 반환
# 성공 code 는 ResponseCode.SUCCESS(0) 가 아니라 Result.ofSuccess 의 HttpStatus.OK(200) 이다
# 결과는 전역변수로 남긴다(서브셸 없이 호출해야 값이 보존됨)
#   API_BODY: 응답 본문, API_CODE: Result.code(특정 실패 code 구분용)
API_BODY=''
API_CODE=''
api_call() {
  local method="$1" url="$2" body="${3:-}" raw status
  local args=(-sS --max-time 30 -X "$method" -w '\n%{http_code}')
  # 본문이 있으면 JSON 으로 보낸다
  [ -z "$body" ] || args+=(-H 'Content-Type: application/json' --data "$body")
  raw="$(curl "${args[@]}" "$url" || true)"
  # 마지막 줄이 HTTP 상태, 그 앞이 본문
  status="$(tail -n1 <<<"$raw")"
  API_BODY="$(sed '$d' <<<"$raw")"
  API_CODE="$(jq -r '.code // empty' 2>/dev/null <<<"$API_BODY" || true)"
  if [ "$status" != "200" ] || [ "$API_CODE" != "200" ]; then
    fail "$method $url → HTTP ${status:-?}, code ${API_CODE:-?}, $(jq -r '.message // empty' \
      2>/dev/null <<<"$API_BODY" || true)"
    return 1
  fi
}

# 서버가 응답할 때까지 1초 간격으로 대기(기본 30초). 인증 없이 열린 영화 목록 API 로 확인
wait_for_server() {
  local base="$1" limit="${2:-30}" i code
  for ((i = 1; i <= limit; i++)); do
    code="$(curl -s -o /dev/null -w '%{http_code}' "$base/api/v1/movie/" || true)"
    [ "$code" = "200" ] && { info "서버 응답 확인: $base"; return 0; }
    sleep 1
  done
  fail "서버가 ${limit}초 안에 응답하지 않습니다: $base"
  exit 1
}
