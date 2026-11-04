---
description: Jib 로 이미지를 빌드하고 docker compose(dev 프로파일)로 로컬 배포한다
allowed-tools: Bash(./gradlew*), Bash(docker*), Bash(lsof*), Bash(kill*), Bash(curl*), Bash(sleep*)
---

# 로컬 배포 (Jib + docker compose)

`cinema-middleware` 를 Jib 로 Docker 이미지(`cinema-middleware:latest`, arm64)로 빌드하고,
`docker-compose.yml`(MySQL·Redis) + `docker-compose-dev.yml`(앱) 조합으로 컨테이너를 띄운다.
테스트는 실행하지 않는다.

## 절차

1. **8080 포트 점유 확인 및 정리**
   - `lsof -nP -iTCP:8080 -sTCP:LISTEN` 으로 점유 프로세스를 확인한다.
   - 점유 프로세스가 `cinema-middleware` 컨테이너(docker 프록시, `com.docker` / `vpnkit` 등)라면
     종료하지 않는다. 4단계 `up -d` 가 컨테이너를 교체한다.
   - 그 외(로컬 `./gradlew bootRun` 등 Java 프로세스)는 `kill <PID>` 로 종료하고,
     최대 10초 동안 포트가 풀리는지 다시 확인한다. 안 풀리면 `kill -9 <PID>`.
   - 종료한 프로세스(PID·명령)는 최종 보고에 포함한다.

2. **Gradle 데몬 정지 후 이미지 빌드**
   ```bash
   ./gradlew --stop && ./gradlew jibDockerBuild
   ```
   - 실패하면 여기서 중단하고 에러 원인(컴파일 오류, Docker 데몬 미기동 등)을 요약 보고한다.
   - 성공 시 `docker images cinema-middleware` 로 `latest` 태그의 생성 시각을 확인한다.

3. **(빌드 성공 시에만) 컨테이너 기동**
   ```bash
   docker compose -f docker-compose.yml -f docker-compose-dev.yml up -d
   ```
   - 이미지가 바뀌었으면 compose 가 `cinema-middleware` 컨테이너를 재생성한다.
   - MySQL·Redis 는 healthcheck 통과 후 앱이 뜬다(`depends_on: service_healthy`).

4. **배포 검증**
   - 상태: `docker compose -f docker-compose.yml -f docker-compose-dev.yml ps`
     — 세 서비스(mysql, redis, cinema-middleware)가 모두 running 이어야 한다.
   - 기동 대기: 최대 90초 동안 3초 간격으로
     `docker compose -f docker-compose.yml -f docker-compose-dev.yml logs cinema-middleware`
     에서 `Started` 로그(`Started CinemaMiddlewareApplication`)가 나타나는지 확인한다.
     `APPLICATION FAILED TO START` 또는 컨테이너 재시작 반복이 보이면 즉시 실패로 판단한다.
   - 헬스체크: `curl -s -w '\n%{http_code}' http://localhost:8080/actuator/health`
     — HTTP 200 + `{"status":"UP"}` 이면 정상. 503 / `DOWN` 이면 DB·Redis 중 하나가 내려간 것이므로
     실패로 보고하고 mysql·redis 컨테이너 상태 확인을 안내한다.
   - 로그: `logs --tail 50 cinema-middleware` 를 확인해 ERROR/WARN 이 있으면 요약한다.

5. **보고** — 아래를 짧게 정리한다.
   - 종료한 8080 점유 프로세스(있었다면)
   - 빌드 결과 및 이미지 태그·생성 시각
   - 서비스별 컨테이너 상태
   - 기동 소요 시간, 헬스체크 응답 코드
   - 실패 시 원인이 되는 로그 구간

## 주의

- dev 프로파일(`docker-compose-dev.yml` 의 `SPRING_PROFILES_ACTIVE=dev`, `application-dev.yaml`)로 뜬다. SQL 로그가 DEBUG·바인딩 TRACE 라 로그가 많다. 로컬 개발 DB 볼륨(`mysql-data`)을 그대로 쓴다.
- Hibernate `ddl-auto: validate` 이므로 엔티티와 `schema.sql` 이 어긋나면 기동에 실패한다. 이때는 로그의 validate 오류를 보고하고 `schema-sync` 에이전트 사용을 제안한다.
- `docker compose down -v` 처럼 볼륨을 지우는 명령은 실행하지 않는다.
- 비밀번호 등 환경변수 값은 보고에 출력하지 않는다.
