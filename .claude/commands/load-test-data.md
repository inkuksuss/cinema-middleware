---
description: MySQL 에 고정 테스트 데이터를 적재한다 (TRUNCATE 후 재삽입, 멱등)
allowed-tools: Bash(docker compose*), Bash(bash scripts/load-test-data.sh*), Bash(BASE_DATE=* bash scripts/load-test-data.sh*)
---

# 테스트 데이터 적재

`cinema-middleware` 로컬 MySQL 에 `docs/test-data.sql` 의 고정 테스트 데이터를 적재한다.
스크립트가 17개 테이블을 전부 TRUNCATE 한 뒤 재삽입하므로 몇 번을 실행해도 결과가 같다.

## 절차

1. docker compose 인프라 상태 확인: `docker compose ps`
   - `mysql` 서비스가 미기동이면 스크립트가 자동으로 `docker compose up -d mysql` 한다.
2. 적재 스크립트 실행: `bash scripts/load-test-data.sh`
   - 기준일을 바꾸려면: `BASE_DATE=2026-12-25 bash scripts/load-test-data.sh`
   - DB 접속 정보는 저장소 루트 `.env`(`MYSQL_DATABASE`·`MYSQL_USER`·`MYSQL_PASSWORD`)에서 읽는다.
     `.env` 가 없으면 스크립트가 중단하므로 `cp .env.example .env` 후 값을 채우라고 안내한다.
3. 스크립트 출력에서 다음을 확인해 사용자에게 요약 보고한다:
   - **행수** — member 10,000 / movie 36 / theater 20 / theater_seat 6,000 / screening 1,050 / 예매 약 2.2만
   - **정합성 검사** — `v` 컬럼이 전부 0이어야 정상 (좌석·회차·예매·결제·쿠폰·FK 고아행·보조 테이블 잔재)
   - **상태 분포** — 회차 END_SALE/ON_SALE/PRE_SALE/SOLD_OUT(3), 예매 SUCCESS ~84% / CANCELED ~10% / EXPIRED ~5% / PENDING ~1%
4. 앱(`./gradlew bootRun`)이 떠 있어도 재기동 불필요하다. 테이블 구조는 그대로고 데이터만 바뀐다.

## 주의

- **로컬 개발 DB 전용이다. 운영 DB 에 실행하지 않는다.**
- `docs/test-data.sql` 의 기준일은 파일 상단 `SET @today` / `SET @now` 두 줄이다.
  "오늘" 기준 상대 날짜 시나리오(과거 상영 END_SALE, 임박 ON_SALE, 미래 PRE_SALE 등)는
  이 기준일을 따른다. `BASE_DATE` 환경변수를 주면 스크립트가 이 두 줄을 임시로 치환한다.
- 스키마(테이블)는 이 스크립트가 만들지 않는다. 테이블이 15개 미만이면 `schema.sql` 을
  먼저 적재한다.
- 로그인 계정: user@ / vip@ / admin@ / super@example.com, 전원 비밀번호 `test1234!`.
