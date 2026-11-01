# 테스트 데이터

## 적재

```bash
bash scripts/load-test-data.sh                          # 또는 /load-test-data
BASE_DATE=2026-12-25 bash scripts/load-test-data.sh     # 기준일 변경
```

- `docs/test-data.sql` 을 적재한다. 17개 테이블을 TRUNCATE 후 재삽입하므로 멱등이다. **로컬 DB 전용.**
- 스크립트가 정합성 검사(좌석·예매·결제·쿠폰·FK)를 출력한다. `v` 컬럼이 전부 0이어야 정상.
- 날짜는 파일 상단 `SET @today` / `SET @now` 기준 상대값이다(`BASE_DATE` 가 이 두 줄을 치환).
- 엔티티·스키마를 바꾸면 `docs/test-data.sql` 과 스크립트의 검증 쿼리도 함께 맞춘다.

## 데이터 개요 (상세 규칙은 `docs/test-data.sql` 상단 주석)

- 로그인 계정(비밀번호는 `docs/test-data.sql` 상단 주석 참고): `user@`(COMMON) · `vip@`(VIP) · `admin@`(MANAGE_ADMIN) · `super@example.com`(SUPER_ADMIN)
- 회원 10,000 / 영화 36 / 영화관 3 · 상영관 20 · 관당 300석 / 회차 1,050(기준일 -7 ~ +6일) / 예매 약 2.2만
- 회차 상태: 시작 전 3일 이내 `ON_SALE`, 그 이후 `PRE_SALE`, 시작된 회차 `END_SALE`, 매진 3개 `SOLD_OUT`.
