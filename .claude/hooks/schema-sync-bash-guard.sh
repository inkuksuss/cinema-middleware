#!/usr/bin/env bash
# schema-sync 에이전트 전용 PreToolUse(Bash) 훅. 동기화와 무관한 파괴적 명령을 막는다.
# ALTER/CREATE/DROP TABLE 은 허용한다(허가 여부는 에이전트 규칙으로 판단). 차단은 exit 2 + stderr.
set -uo pipefail

cmd="$(jq -r '.tool_input.command // empty')"         # 실행하려는 명령 (stdin JSON)
[ -n "$cmd" ] || exit 0

block() { echo "schema-sync 에서 허용하지 않는 명령입니다: $1" >&2; exit 2; }

# 대소문자 무시 비교용
lc="$(printf '%s' "$cmd" | tr '[:upper:]' '[:lower:]')"

# 데이터 전체 삭제 계열 (테스트 데이터 재적재는 scripts/load-test-data.sh 로만)
printf '%s' "$lc" | grep -qE '\btruncate\b'                    && block "TRUNCATE"
printf '%s' "$lc" | grep -qE '\bdelete[[:space:]]+from\b'      && block "DELETE FROM"
printf '%s' "$lc" | grep -qE '\bdrop[[:space:]]+(database|schema)\b' && block "DROP DATABASE/SCHEMA"

# 볼륨·컨테이너 파괴
printf '%s' "$lc" | grep -qE 'compose[^|;&]*down[^|;&]*(-v\b|--volumes)' && block "docker compose down -v"
printf '%s' "$lc" | grep -qE 'docker[[:space:]]+(volume[[:space:]]+rm|system[[:space:]]+prune|rm\b)' && block "docker 볼륨/컨테이너 삭제"

# git 쓰기 명령
printf '%s' "$lc" | grep -qE '\bgit[[:space:]]+(commit|push|reset|checkout|restore|rebase|merge|stash|clean|rm|mv|add|switch)\b' \
  && block "git 쓰기 명령"

# 셸로 파일 수정·삭제 (파일 수정은 Edit/Write 로만, 훅이 대상을 제한한다)
printf '%s' "$lc" | grep -qE '\bsed[[:space:]]+(-[a-z]*i|--in-place)|\brm[[:space:]]+-|\bmv[[:space:]]|\btee[[:space:]]' \
  && block "셸을 통한 파일 수정·삭제"

# 로컬이 아닌 DB 접속 차단 (-h/--host 가 localhost·127.0.0.1·컨테이너명 mysql 이 아니면)
# mysql 클라이언트가 있는 구간(|, ;, & 로 나눈 조각)만 본다 — grep -h 같은 다른 옵션 오탐 방지
hosts="$(printf '%s\n' "$lc" | tr '|;&' '\n\n\n' \
         | grep -E '(^|[[:space:]/])mysql(dump|admin)?([[:space:]]|$)' \
         | grep -oE '[[:space:]](-h|--host)([[:space:]=]+)?[^[:space:]-][^[:space:]]*' \
         | sed -E 's/^[[:space:]]*(-h|--host)([[:space:]=]+)?//' || true)"
for h in $hosts; do
  case "$h" in localhost|127.0.0.1|mysql|\$*|\"*) ;; *) block "로컬이 아닌 DB 호스트($h)" ;; esac
done

exit 0
