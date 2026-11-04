#!/usr/bin/env bash
# =============================================================================
# .env 로더 (source 전용 — 직접 실행하지 않는다)
#
#   사용 예: source scripts/dotenv.sh && load_dotenv          # 저장소 루트의 .env
#           load_dotenv path/to/file
#
#   - KEY=VALUE 줄만 읽어 셸 변수로 설정한다. 빈 줄·# 주석·형식이 다른 줄은 건너뛴다.
#   - export 하지 않는다. 비밀번호가 node·k6 등 자식 프로세스 환경에 퍼지지 않게 하고,
#     필요한 값만 호출 스크립트가 명시적으로 넘긴다(예: DB_PASS="$DB_PASS" bash reset.sh).
#   - 파일을 셸로 실행(source)하지 않는다. 값에 & ? 같은 문자가 있어도 그대로 들어간다.
#   - 셸에 이미 지정된 변수는 덮어쓰지 않는다(환경변수 > .env).
#   - 파일이 없으면 아무것도 하지 않는다. 필수 값 검사는 호출하는 스크립트가 한다.
# =============================================================================

load_dotenv() {
  local file="${1:-.env}" line key
  [ -f "$file" ] || return 0
  while IFS= read -r line || [ -n "$line" ]; do
    # 빈 줄·주석 건너뜀
    [[ "$line" =~ ^[[:space:]]*(#|$) ]] && continue
    key="${line%%=*}"
    # 변수 이름 형식이 아니면 건너뜀
    [[ "$line" == *=* && "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || continue
    # 이미 지정된 환경변수는 유지
    [ -n "${!key+x}" ] && continue
    printf -v "$key" '%s' "${line#*=}"
  done < "$file"
}
