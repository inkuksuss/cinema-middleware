# seat-race 좌석 선점 경쟁

## 실행 방법
- 사전: docker compose·서버, node·k6·jq·htpasswd
- `DB_PASS=... ./scripts/load/seat-race/run.sh`
- 매 실행 회원 15만 신규 SQL 적재 → 로그인 → 상영관 3개×1000석 회차
- 환경변수: TOTAL(150000) RATE(5000) DURATION(30s) PRE_VUS(2000) MAX_VUS(5000) SEATS(1000)
- 종료 시 reset.sh 가 테스트 데이터 삭제(KEEP_DATA=1: 남김)
- 수동 정리: `DB_PASS=... bash scripts/load/seat-race/reset.sh [--dry-run]`

## 참고사항
- PASS: 회차별 1000석 RESERVED, 13·28 외 응답 0, 응답 150,000건, dropped 0
- RATE×초 = TOTAL(1인 1회)
- SQL 로그 병목 시 로그 레벨 낮춤
