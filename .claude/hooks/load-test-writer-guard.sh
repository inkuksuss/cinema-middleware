#!/usr/bin/env bash
# load-test-writer 에이전트 전용 PreToolUse 훅. Write/Edit 를 scripts/load/ 의 .sh·.js 로 제한한다.
# 차단은 exit 2 + stderr. Claude Code 가 도구 호출을 막고 이 메시지를 에이전트에게 전달한다.
set -uo pipefail

root="${CLAUDE_PROJECT_DIR:-$(pwd)}"                  # 프로젝트 루트
path="$(jq -r '.tool_input.file_path // empty')"      # 쓰려는 파일 경로 (stdin JSON)

# 경로를 못 읽으면(jq 없음 포함) 안전하게 차단
[ -n "$path" ] || { echo "파일 경로를 확인할 수 없어 차단합니다." >&2; exit 2; }

case "$path" in /*) ;; *) path="$root/$path" ;; esac  # 상대 경로는 루트 기준 절대 경로로

# ../ 등으로 허용 디렉터리를 빠져나가는 경로 차단
case "$path" in
  *"/../"*|*"/./"*) echo "경로에 .. 또는 . 이 있어 차단합니다: $path" >&2; exit 2 ;;
esac

# 허용 디렉터리: scripts/load/ 아래만 (애플리케이션 코드·설정 수정 금지)
case "$path" in
  "$root"/scripts/load/*) ;;
  *) echo "scripts/load/ 밖은 수정할 수 없습니다: $path" >&2; exit 2 ;;
esac

# 허용 확장자: .sh, .js 만. 그 외는 사용자에게 제안만 한다
case "$path" in
  *.sh|*.js) exit 0 ;;
  *) echo ".sh·.js 외 확장자는 만들 수 없습니다: $path (필요하면 사용자에게 제안)" >&2; exit 2 ;;
esac
