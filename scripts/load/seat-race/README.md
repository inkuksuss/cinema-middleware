# seat-race 좌석 선점 경쟁

## 실행 방법
- 사전: 서버 실행, 테스트 데이터, node·k6·jq
- `LOAD_TEST_PASSWORD=... ./scripts/load/seat-race/run.sh`
- seed.js 가 상영관·좌석 300·ON_SALE 회차(2099-12-31) 생성
- 준비만: `node scripts/load/seat-race/seed.js`
- 환경변수: TOTAL(50000) RATE(5000) VUS(5000)
  SCREENING_ID(없으면 생성) CINEMA_ID(1) MOVIE_ID(자동)
- 준비 실패 시: `bash scripts/load/seat-race/cleanup.sh --theater <관id>`

## 참고사항
- PASS: 300석 RESERVED, 예상 밖 응답 0건, 응답 50,000건
- TOTAL 은 VUS 의 배수
- 부하 후 데이터는 남겨 둠. 매번 새 상영관이라 다음 실행에 영향 없음
