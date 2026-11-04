# 실행 · 빌드 · 설정

## 명령어

```bash
cp .env.example .env                  # 최초 1회: 접속 정보 env 파일 준비(아래 "env 파일" 참고)
docker compose up -d                  # MySQL 8.4 + Redis 8.0 (호스트 포트는 .env). 앱 실행 전에 필요
./gradlew bootRun                     # 앱 실행 (:8080)
./gradlew test                        # 전체 테스트 (JUnit 5)
./gradlew test --tests "com.example.cinema_middleware.v1.service.ScreeningServiceTest"
./gradlew compileJava                 # QueryDSL Q 클래스 재생성 (build/generated/)
./gradlew jibDockerBuild              # 로컬 이미지 빌드 → cinema-middleware:latest, :0.0.1-SNAPSHOT
docker compose -f docker-compose.yml -f docker-compose-dev.yml up -d    # 앱 컨테이너(dev 프로파일) 실행 — /local-deploy
docker compose -f docker-compose.yml -f docker-compose-prod.yml up -d   # 앱 컨테이너(prod 프로파일) 실행
bash scripts/load-test-data.sh        # 테스트 데이터 적재 → test-data.md
```

- 린터·포매터 설정은 없다.
- **Dockerfile 이 없다.** 이미지는 Gradle Jib 플러그인이 만든다
  (base `eclipse-temurin:21-jre`, `linux/arm64` 고정, JVM `-Xms512m -Xmx512m`).
- **프로파일은 이미지에 고정하지 않는다.** compose 의 `SPRING_PROFILES_ACTIVE` 로 정한다
  (`docker-compose-dev.yml` → dev, `docker-compose-prod.yml` → prod). jvmFlags 에 `-Dspring.profiles.active` 를
  넣으면 환경변수보다 우선해 compose 설정이 무시되므로 넣지 않는다. compose 없이 이미지를 띄우면 기본 프로파일이다.
- `docker-compose-dev.yml`·`docker-compose-prod.yml` 은 앱 서비스만 정의하고 `mysql`·`redis` 서비스의 healthy 상태에 의존한다.
  인프라 정의는 `docker-compose.yml` 에 있으므로 함께 `-f` 로 지정한다.

## env 파일 (접속 정보)

compose 파일에는 접속 정보 값을 쓰지 않는다. 모든 값은 저장소 루트의 env 파일에 둔다.

| 파일 | git | 읽는 쪽 | 키 |
| --- | --- | --- | --- |
| `.env.example` | 커밋 | 템플릿(키만, 값 비움) | 아래 전부 |
| `.env` | 제외 | compose 변수 치환(자동 로드), mysql `env_file`, `scripts/` | `MYSQL_ROOT_PASSWORD`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_HOST_PORT`, `REDIS_HOST_PORT`, `APP_HOST_PORT` |
| `.env.dev` | 제외 | `docker-compose-dev.yml` 앱 `env_file` | `SPRING_DATASOURCE_URL`·`_USERNAME`·`_PASSWORD`, `SPRING_DATA_REDIS_HOST`·`_PORT` |
| `.env.prod` | 제외 | `docker-compose-prod.yml` 앱 `env_file` | 〃 |

- 파일이 없거나 포트 키가 비면 compose 가 바로 오류를 낸다(`env_file` 기본 required, 포트는 `${...:?}`).
- **mysql-data 볼륨은 최초 기동 때의 `MYSQL_*` 값으로 계정이 만들어진다.** 이후 `.env` 값을 바꿔도 DB 계정은 그대로라
  접속이 실패한다. 바꾸려면 DB 에서 직접 계정을 변경하거나 볼륨을 재생성한다.
- 로컬은 dev·prod 앱이 같은 MySQL 을 쓰므로 `.env.dev`·`.env.prod` 의 계정은 `.env` 의 `MYSQL_USER`·`MYSQL_PASSWORD` 와 같아야 한다.
  앱 컨테이너는 compose 네트워크 안에서 서비스 이름(`mysql`, `redis`)과 컨테이너 포트(3306, 6379)로 접속한다.
- mysql healthcheck 는 컨테이너 환경변수 `$MYSQL_ROOT_PASSWORD` 를 쓴다(compose 파일에 값 없음).
- 스크립트(`scripts/load-test-data.sh`, `scripts/load/**`)는 `scripts/dotenv.sh` 의 `load_dotenv` 로 `.env` 를 읽는다.
  파일을 실행하지 않고 `KEY=VALUE` 만 파싱하며, export 하지 않고, 이미 지정된 환경변수(`DB_PASS` 등)가 우선한다.

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
- `application*.yaml` 은 `.gitignore`(`/src/**/*.yaml`) 대상이라 저장소에 없다. 값은 로컬 파일과 env 파일(`.env*`)을 본다.
- `jwt.*` 는 `JwtProperties`(record, `@ConfigurationPropertiesScan`)로 바인딩된다.
- enum 입력은 대소문자를 구분하지 않는다 — JSON 본문은 `jackson.mapper.accept-case-insensitive-enums`,
  쿼리·경로 파라미터는 `WebConfig` 에 등록한 `StringToEnumConverterFactory`(빈 문자열 → null).
- `spring-boot-starter-kafka` 의존성은 있지만 코드에서 쓰는 곳은 없다.
- actuator 는 `health` 만 노출한다(`management.endpoints.web.exposure.include`, `show-details: never`).
  DB·Redis 인디케이터가 포함되어 하나라도 DOWN 이면 503. `/local-deploy` 의 헬스체크가 이 엔드포인트를 쓴다.

### 타임아웃 (`application.yaml`, prod 상속)
원칙은 "혼잡·장애 시 오래 붙잡지 말고 빨리 실패 안내"다. 시간 초과는 HTTP 503 + `SERVICE_UNAVAILABLE`(28)로 응답한다.

| 항목 | 값 | 기본값 | 이유 |
| --- | --- | --- | --- |
| `server.tomcat.connection-timeout` | 20s | 60s | 연결 후 요청 데이터가 안 오는 클라이언트를 일찍 끊는다 |
| `server.tomcat.keep-alive-timeout` | 65s | connection-timeout 과 같음 | 앞단 ALB idle(기본 60s)보다 길어야 502 를 막는다. **ALB idle 을 바꾸면 이 값도 그보다 길게** |
| `spring.datasource.hikari.connection-timeout` | 3000ms | 30s | 풀 대기. 초과 시 503 |
| `hikari.data-source-properties.connectTimeout` | 1000ms | 무제한 | DB TCP 연결(같은 네트워크) |
| `hikari.data-source-properties.socketTimeout` | 30000ms | 무제한 | 끝나지 않는 쿼리 방지. 30초 넘는 작업이 생기면 재검토 |
| `hikari.data-source-properties.sessionVariables` | `innodb_lock_wait_timeout=5` | 50s | 이 앱 연결에만 행 잠금 대기 5초 |
| `spring.data.redis.connect-timeout` / `timeout` | 1s / 1s | 10s / 60s | 매 인증 요청이 블랙리스트를 조회하므로 짧게 |
| `MovieReservationService.addReservation` | `@Transactional(timeout = 5)` | 없음 | 좌석 경쟁 경로 |

- 드라이버 옵션을 URL 이 아니라 `data-source-properties` 에 둔 이유: 운영 URL 은 `SPRING_DATASOURCE_URL` 환경변수가 덮어쓴다.
- Redis 블랙리스트 조회 실패 시 `JwtAuthenticationFilter` 가 요청을 막는다(fail-closed, 503 + 28). 토큰 없는 요청은 영향 없음.
- 좌석 INSERT 중 잠금 대기 초과·교착·시간 초과는 `SeatService.putIfNotTaken` 이 `SeatAlreadyTakenException`(13)으로 바꾼다.

## 시크릿 유출 방지

- **문서·코드·커밋 메시지에 비밀번호·키 값을 쓰지 않는다.** 파일 경로와 키 이름만 적는다.
- `scripts/check-secrets.sh` 가 스테이징된 변경의 추가된 줄에서 시크릿을 찾아 커밋을 막는다
  (파일명, `키: 값` 패턴, `application*.yaml`·`.env*`·`docker-compose*.yml` 에 있는 실제 값).
- 훅 활성화(클론마다 1회): `git config core.hooksPath .githooks` — 이 설정은 로컬 전용이라 새로 클론하면 다시 해야 한다.
- 스테이징 전 사전 점검: `bash scripts/check-secrets.sh <파일...>`
- 의도한 줄은 같은 줄에 `secret-ok` 를 적으면 통과한다. `git commit --no-verify` 로는 우회되므로 쓰지 않는다.
- 8자 미만의 값은 실제 값 대조에서 제외된다.
