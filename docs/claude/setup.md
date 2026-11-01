# 실행 · 빌드 · 설정

## 명령어

```bash
docker compose up -d                  # MySQL 8.4(:13306) + Redis 8.0(:16379). 앱 실행 전에 필요
./gradlew bootRun                     # 앱 실행 (:8080)
./gradlew test                        # 전체 테스트 (JUnit 5)
./gradlew test --tests "com.example.cinema_middleware.v1.service.ScreeningServiceTest"
./gradlew compileJava                 # QueryDSL Q 클래스 재생성 (build/generated/)
./gradlew jibDockerBuild              # 로컬 이미지 빌드 → cinema-middleware:latest, :0.0.1-SNAPSHOT
docker compose -f docker-compose-prod.yml up -d   # 앱 컨테이너(prod 프로파일) 실행
bash scripts/load-test-data.sh        # 테스트 데이터 적재 → test-data.md
```

- 린터·포매터 설정은 없다.
- **Dockerfile 이 없다.** 이미지는 Gradle Jib 플러그인이 만든다
  (base `eclipse-temurin:21-jre`, `linux/arm64` 고정, JVM `-Xms512m -Xmx512m -Dspring.profiles.active=prod`).
- `docker-compose-prod.yml` 은 앱 서비스만 정의하고 `mysql`·`redis` 서비스의 healthy 상태에 의존한다.
  인프라는 `docker-compose.yml` 로 먼저 올린다.

## 스키마

- `src/main/resources/schema.sql` 이 단일 소스다. 기본 프로파일은 부팅 시 `spring.sql.init.mode: always` 로
  이 스크립트를 실행하고, Hibernate 는 `ddl-auto: validate` 로 엔티티 매핑만 검증한다.
- 모든 DDL 이 `CREATE TABLE IF NOT EXISTS` 다. 컬럼 추가·타입 변경은 재기동으로 반영되지 않는다.
  마이그레이션 도구가 없으므로 DB 에 수동 반영하거나 `docker compose down -v` 로 재생성한다.
- 엔티티를 바꾸면 `schema.sql` 도 함께 고친다(불일치 시 부팅과 모든 `@SpringBootTest` 실패).
- `application-prod.yaml` 에는 `spring.sql.init` 설정이 없어 prod 에서는 `schema.sql` 이 실행되지 않는다.
- 테이블·컬럼 설명은 `docs/SCHEMA.md`.

## 설정 (`application.yaml` / `application-prod.yaml`)

| 항목 | 기본 | prod |
| --- | --- | --- |
| access / refresh 토큰 만료(초) | 1800 / 86400 | 600 / 1800 |
| DB 계정 | 각 yaml 의 `spring.datasource.*` (값은 문서에 적지 않는다) | 〃 |
| `schema.sql` 실행 | O | X |
| multipart 제한 | 파일 3MB / 요청 5MB | 미설정 |

- 공통: `open-in-view: false`, SQL 로그 DEBUG + 바인딩 TRACE.
- 환경변수로 오버라이드: `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD`,
  `SPRING_DATA_REDIS_HOST` / `_PORT`, `JWT_SECRET`. 기본값이 yaml 에 하드코딩되어 있으니 운영 시크릿을 커밋하지 않는다.
- `application*.yaml` 은 `.gitignore`(`/src/**/*.yaml`) 대상이라 저장소에 없다. 값은 로컬 파일과 `docker-compose*.yml` 을 본다.
- `jwt.*` 는 `JwtProperties`(record, `@ConfigurationPropertiesScan`)로 바인딩된다.
- enum 입력은 대소문자를 구분하지 않는다 — JSON 본문은 `jackson.mapper.accept-case-insensitive-enums`,
  쿼리·경로 파라미터는 `WebConfig` 에 등록한 `StringToEnumConverterFactory`(빈 문자열 → null).
- `spring-boot-starter-kafka` 의존성은 있지만 코드에서 쓰는 곳은 없다.

## 시크릿 유출 방지

- **문서·코드·커밋 메시지에 비밀번호·키 값을 쓰지 않는다.** 파일 경로와 키 이름만 적는다.
- `scripts/check-secrets.sh` 가 스테이징된 변경의 추가된 줄에서 시크릿을 찾아 커밋을 막는다
  (파일명, `키: 값` 패턴, `application*.yaml`·`.env*`·`docker-compose*.yml` 에 있는 실제 값).
- 훅 활성화(클론마다 1회): `git config core.hooksPath .githooks` — 이 설정은 로컬 전용이라 새로 클론하면 다시 해야 한다.
- 스테이징 전 사전 점검: `bash scripts/check-secrets.sh <파일...>`
- 의도한 줄은 같은 줄에 `secret-ok` 를 적으면 통과한다. `git commit --no-verify` 로는 우회되므로 쓰지 않는다.
- 8자 미만의 값은 실제 값 대조에서 제외된다.
