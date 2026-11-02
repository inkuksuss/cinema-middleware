#!/usr/bin/env bash
# schema-sync 에이전트 전용 PreToolUse(Write|Edit) 훅. 동기화 대상 4개 파일만 수정을 허용한다.
# 차단은 exit 2 + stderr. Claude Code 가 도구 호출을 막고 이 메시지를 에이전트에게 전달한다.
set -uo pipefail

root="${CLAUDE_PROJECT_DIR:-$(pwd)}"                  # 프로젝트 루트
path="$(jq -r '.tool_input.file_path // empty')"      # 쓰려는 파일 경로 (stdin JSON)

# 경로를 못 읽으면(jq 없음 포함) 안전하게 차단
[ -n "$path" ] || { echo "파일 경로를 확인할 수 없어 차단합니다." >&2; exit 2; }

case "$path" in /*) ;; *) path="$root/$path" ;; esac  # 상대 경로는 루트 기준 절대 경로로

# 허용 목록: 엔티티 기준으로 동기화하는 DB 관련 파일만 (애플리케이션 코드 수정 금지)
case "$path" in
  "$root/src/main/resources/schema.sql" | \
  "$root/docs/SCHEMA.md" | \
  "$root/docs/test-data.sql" | \
  "$root/scripts/load-test-data.sh")
    exit 0 ;;
esac

echo "동기화 대상(schema.sql, docs/SCHEMA.md, docs/test-data.sql, scripts/load-test-data.sh) 외에는 수정할 수 없습니다: $path → 보고서에 제안하세요." >&2
exit 2
