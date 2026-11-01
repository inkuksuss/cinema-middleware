#!/usr/bin/env bash
# =============================================================================
# 시크릿 유출 차단 검사
#
#   bash scripts/check-secrets.sh            # 스테이징된 변경의 "추가된 줄"만 검사 (pre-commit 훅이 호출)
#   bash scripts/check-secrets.sh --pending  # 아직 스테이징하지 않은 변경(수정+신규)까지 전부 커밋한다고 가정하고 검사
#                                            # (임시 인덱스를 쓰므로 실제 인덱스는 건드리지 않는다. /git-commit 이 사용)
#   bash scripts/check-secrets.sh FILE...    # 지정한 파일 전체를 검사 (스테이징 전 사전 점검용)
#
# 검사 항목
#   1. 커밋하면 안 되는 파일명 (.env, 인증서/키, application*.yaml 등)
#   2. 시크릿으로 보이는 "키: 값" / "키=값" 패턴
#   3. gitignore 대상 설정(application*.yaml, .env*)과 docker-compose*.yml 에 들어 있는
#      실제 시크릿 값이 그대로 들어온 줄 — 값을 이 스크립트에 적지 않아도 설정을 따라간다
#
# 의도한 줄은 같은 줄에 `secret-ok` 를 적으면 통과한다.
# 출력에는 값을 찍지 않는다(파일:줄번호와 사유만).
# 한계: 3번은 8자 미만의 값을 제외한다(`cinema` 같은 흔한 단어의 오탐 방지).
# =============================================================================
set -uo pipefail

cd "$(git rev-parse --show-toplevel)" || exit 1

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
: > "$TMP/found"

# --pending: 임시 인덱스에 작업 트리의 모든 변경을 올려 두고, 이하는 스테이징 검사와 같은 경로로 처리한다
if [ "${1:-}" = "--pending" ]; then
  shift
  export GIT_INDEX_FILE="$TMP/pending-index"
  git read-tree HEAD 2>/dev/null || true
  git add -A -- . >/dev/null 2>&1
fi

# -----------------------------------------------------------------------------
# 패턴 (heredoc 으로 두어 셸 따옴표 문제를 피한다)
# -----------------------------------------------------------------------------
cat > "$TMP/pat_other" <<'EOF'
(password|passwd|pwd|secret|api[_-]?key|access[_-]?key|private[_-]?key|credentials?|token)["']?[[:space:]]*[:=][[:space:]]*["`']?[^]"'`$<{(|)[:space:]]{6,}
(비밀번호|패스워드)[^`]{0,30}`[^`/]{6,}`
EOF
# 코드 파일(.java/.js/.ts/.kt)은 따옴표로 감싼 문자열만 값으로 본다 (상수·변수 참조는 통과)
cat > "$TMP/pat_code" <<'EOF'
(password|passwd|pwd|secret|api[_-]?key|access[_-]?key|private[_-]?key|token)["']?[[:space:]]*[:=][[:space:]]*["'`][^"'`$]{6,}["'`]
EOF
# 값이 아닌 것: 자리표시자, 의존성 좌표, 필수 환경변수 참조 ${VAR:?}, k6/node 환경변수 참조
cat > "$TMP/placeholder" <<'EOF'
[:=][[:space:]]*["`']?(changeme|your[-_ ]|xxx|\*\*\*|example)
['"][A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+:[0-9][A-Za-z0-9_.-]*['"]
\$\{[A-Za-z0-9_]+:\?
(__ENV|process\.env)\.
EOF

# -----------------------------------------------------------------------------
# 검사 대상 수집
# -----------------------------------------------------------------------------
if [ "$#" -gt 0 ]; then
  for f in "$@"; do printf '%s\n' "$f"; done > "$TMP/names"
  for f in "$@"; do
    if [ -f "$f" ] && grep -Iq . "$f" 2>/dev/null; then
      awk -v f="$f" '{ printf "%s\t%d\t%s\n", f, NR, $0 }' "$f"
    fi
  done > "$TMP/all.tsv"
else
  git diff --cached --name-only --diff-filter=ACMR > "$TMP/names"
  # 추가된 줄만 "파일<TAB>줄번호<TAB>내용" 으로 뽑는다
  git diff --cached -U0 --no-color --diff-filter=ACMR | awk '
    /^\+\+\+ b\// { f = substr($0, 7); next }
    /^@@ /        { match($0, /\+[0-9]+/); n = substr($0, RSTART + 1, RLENGTH - 1) + 0; next }
    /^\+/         { printf "%s\t%d\t%s\n", f, n, substr($0, 2); n++ }
  ' > "$TMP/all.tsv"
fi

# 이 검사 도구 자신과 `secret-ok` 표시가 있는 줄은 제외
awk -F'\t' '$1 !~ /^(scripts\/check-secrets\.sh|\.githooks\/)/ && index($0, "secret-ok") == 0' \
  "$TMP/all.tsv" > "$TMP/lines.tsv"
awk -F'\t' '$1 ~ /\.(java|js|mjs|ts|kt)$/'  "$TMP/lines.tsv" > "$TMP/code.tsv"
awk -F'\t' '$1 !~ /\.(java|js|mjs|ts|kt)$/' "$TMP/lines.tsv" > "$TMP/other.tsv"

# tsv 를 내용/위치 파일로 쪼개 둔다 (grep -n 의 줄번호로 위치를 되찾기 위함)
prep() { cut -f3- "$1" > "$TMP/content"; cut -f1,2 "$1" > "$TMP/meta"; }

# stdin: content 기준 줄번호 목록 → 파일:줄번호 와 사유를 기록
emit() {
  local n
  while IFS= read -r n; do
    [ -n "$n" ] || continue
    printf '  ✗ %s:%s  — %s\n' \
      "$(sed -n "${n}p" "$TMP/meta" | cut -f1)" \
      "$(sed -n "${n}p" "$TMP/meta" | cut -f2)" "$1" >> "$TMP/found"
  done
}

# -----------------------------------------------------------------------------
# 1) 파일명
# -----------------------------------------------------------------------------
while IFS= read -r name; do
  [ -n "$name" ] || continue
  case "$name" in .env.example|*/.env.example) continue ;; esac
  if printf '%s' "$name" | grep -qiE '(^|/)\.env(\..+)?$|\.(pem|key|p12|pfx|jks|keystore)$|(^|/)id_(rsa|ed25519)$|(^|/)credentials[^/]*$|(^|/)application[^/]*\.(ya?ml|properties)$'; then
    printf '  ✗ %s  — 커밋하면 안 되는 파일(시크릿/설정/키)\n' "$name" >> "$TMP/found"
  fi
done < "$TMP/names"

# -----------------------------------------------------------------------------
# 2) 시크릿으로 보이는 패턴
# -----------------------------------------------------------------------------
if [ -s "$TMP/other.tsv" ]; then
  prep "$TMP/other.tsv"
  grep -n -i -E -f "$TMP/pat_other" "$TMP/content" 2>/dev/null \
    | grep -v -i -E -f "$TMP/placeholder" | cut -d: -f1 \
    | emit "시크릿으로 보이는 '키: 값' 패턴"
fi
if [ -s "$TMP/code.tsv" ]; then
  prep "$TMP/code.tsv"
  grep -n -i -E -f "$TMP/pat_code" "$TMP/content" 2>/dev/null \
    | grep -v -i -E -f "$TMP/placeholder" | cut -d: -f1 \
    | emit "소스에 하드코딩된 시크릿으로 보이는 문자열"
fi

# -----------------------------------------------------------------------------
# 3) 설정 파일의 실제 시크릿 값이 그대로 들어왔는지
# -----------------------------------------------------------------------------
SOURCES=""
for f in src/*/resources/application*.yaml src/*/resources/application*.yml \
         src/*/resources/application*.properties .env .env.* docker-compose*.yml docker-compose*.yaml; do
  [ -f "$f" ] || continue
  case "$f" in *.example) continue ;; esac
  SOURCES="$SOURCES $f"
done

if [ -n "$SOURCES" ]; then
  # shellcheck disable=SC2086
  grep -hiE '^[[:space:]]*-?[[:space:]]*[A-Za-z0-9_.-]*(password|passwd|secret|token|api[_-]?key|access[_-]?key|private[_-]?key)[A-Za-z0-9_.-]*[[:space:]]*[:=]' $SOURCES 2>/dev/null \
    | grep -vE '^[[:space:]]*#' \
    | sed -E 's/^[^:=]*[:=][[:space:]]*//' \
    | sed -E 's/[[:space:]]+#.*$//' \
    | sed -E 's/^"(.*)"$/\1/' \
    | sed -E "s/^'(.*)'\$/\\1/" \
    | sed -E 's/^\$\{[A-Za-z0-9_.]+:-?(.*)\}$/\1/' \
    | grep -vE '^[$<{]' \
    | awk 'length($0) >= 8' | sort -u > "$TMP/values"

  if [ -s "$TMP/values" ] && [ -s "$TMP/lines.tsv" ]; then
    prep "$TMP/lines.tsv"
    grep -n -F -f "$TMP/values" "$TMP/content" 2>/dev/null | cut -d: -f1 \
      | emit "설정 파일(application*.yaml/.env*/compose)에 있는 실제 시크릿 값 포함"
  fi
fi

# -----------------------------------------------------------------------------
# 결과
# -----------------------------------------------------------------------------
if [ -s "$TMP/found" ]; then
  {
    echo "[check-secrets] 시크릿 유출 가능성이 있는 변경을 차단했습니다:"
    sort -u "$TMP/found"
    echo "→ 값은 지우고 파일·키 이름만 적으세요. 의도한 줄은 같은 줄에 secret-ok 를 적으면 통과합니다."
  } >&2
  exit 1
fi
exit 0
