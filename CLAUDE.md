# cinema-middleware

영화관 예매 백엔드 API (Spring Boot 4.1 · Java 21 · JPA/QueryDSL · MySQL · Redis)

## 필수 규칙
- 앱 실행 전 `docker compose up -d`
- 스키마 단일 소스는 `schema.sql`. 엔티티 변경 시 함께 수정
- 응답은 `Result<T>`, 새 예외는 `GlobalExceptionAdvice` 에 등록
- `docs/HISTORY.md` 절대 수정 금지
- 비밀번호·키 값은 문서에 쓰지 않는다 (파일·키 이름만)

## 상세 문서 (작업 전 해당 파일을 읽을 것)
`docs/claude/` 아래
- `setup.md` 명령어·설정·스키마
- `architecture.md` 패키지·계층·응답/예외·API
- `domain.md` 엔티티·리포지토리
- `auth.md` JWT 인증
- `test-data.md` 테스트 데이터
