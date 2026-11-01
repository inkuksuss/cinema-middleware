# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 개요

`cinema-middleware`는 영화관(cinema) 예매 서비스의 **백엔드 API 서버**다. Spring Boot 4.1 · Java 21 · Gradle. REST API(`/api/v1/*`)를 제공하고, 프론트엔드는 `../cinema-front`(사용자), `../cinema-admin`(관리자)이다.

현재 인증(회원가입/로그인/토큰 재발급/로그아웃)과 도메인 엔티티 정의까지 구현되어 있고, 영화 조회 API 등은 골격만 있는 초기 개발 단계다.

## 명령어

표준 Gradle 태스크(`bootRun`·`test`·`build`)를 그대로 쓴다. 포트·이미지 등 세부는 `docker-compose.yml`과 `build.gradle`을 본다. 아래는 **추측으로 알 수 없는 것들**이다.

- 🔴 **로컬 실행 순서**: `docker compose up -d` → `./gradlew bootRun`. **인프라가 없으면 기동에 실패한다.**
- QueryDSL `Q*` 재생성이 필요하면 `./gradlew compileJava`.
- 이미지 빌드는 `./gradlew jibDockerBuild` — **Dockerfile이 없다.** Gradle Jib 플러그인이 만든다 (base `eclipse-temurin:21-jre`, arm64 고정).
- 🔴 **스키마는 `src/main/resources/schema.sql` 이 단일 소스다.** 부팅 시 `spring.sql.init`(`mode: always`)이 이 스크립트로 테이블을 생성하고, Hibernate 는 `ddl-auto: validate` 로 엔티티 매핑만 검증한다(불일치 시 부팅·모든 `@SpringBootTest` 실패). 모든 `CREATE TABLE IF NOT EXISTS` 라 재기동은 안전하지만, 스키마 **변경**(컬럼 추가·타입 변경)은 재기동으로 반영되지 않는다 — 마이그레이션 도구(Flyway 등)가 없으므로 `schema.sql` 수정 후 DB 수동 반영 또는 `docker compose down -v`. 엔티티를 바꾸면 `schema.sql` 도 반드시 함께 고친다. (자세한 스키마 설명은 `docs/SCHEMA.md`)
- 린터는 설정되어 있지 않다.

## 설정

- `application.yaml`(기본) / `application-prod.yaml`(prod 프로파일). prod는 토큰 만료 시간이 더 짧다.
- DB·Redis 접속 정보와 `JWT_SECRET` 기본값이 yaml에 하드코딩되어 있고, `SPRING_DATASOURCE_URL` 등 환경변수로 오버라이드된다. **운영 시크릿을 커밋하지 않도록 주의**한다.
- `docker-compose.yml`은 인프라(MySQL 8.4 / Redis 8.0)만, `docker-compose-prod.yml`은 앱 컨테이너 정의(healthcheck 의존)다. prod compose 는 인프라 서비스를 별도로 올려둔 상태를 전제로 한다.

## 아키텍처

패키지는 `com.example.cinema_middleware.v1.*`로 **버전 네임스페이스**를 둔다. 계층: `controller` → `service` → `repository` → `domain.entity`. `@EnableJpaAuditing`, `@ConfigurationPropertiesScan`은 `CinemaMiddlewareApplication`에 있다.

### 인증 (JWT, 무상태)

- `SecurityConfig`: 세션 없이(`STATELESS`), CSRF/formLogin/httpBasic 모두 비활성. `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 끼운다.
- `JwtAuthenticationFilter`: 매 요청마다 `Authorization: Bearer <token>` 헤더를 파싱 → 유효하고 블랙리스트에 없으면 `SecurityContext`에 인증 세팅. **principal 은 `Long memberId`**(엔티티가 아님), authority 는 토큰의 `role` 클레임 하나.
- `AuthService.login`: `AuthenticationManager`(→ `UserDetailsHandler`가 이메일로 `Member` 조회 → `MemberPrincipal` + `BCryptPasswordEncoder`)로 검증 후 access/refresh 토큰 발급.
- `JwtTokenProvider`: HS256, `secret` 은 BASE64 디코드해서 키로 사용. subject = memberId, 클레임에 `email`/`role`. 만료·서명오류는 전부 "무효 토큰"으로 처리(`isValid` false).
- `AuthTokenRepository`: **Redis 접근을 전담**. 키 포맷은 이 클래스 안에만 존재한다.
  - `refresh:{memberId}` — refresh 토큰 저장. 재발급 시 저장값과 정확히 일치해야 통과(재사용 방지: 로그아웃/재발급하면 값이 달라짐).
  - `blacklist:{accessToken}` — 로그아웃 시 access 토큰을 남은 TTL 만큼 블랙리스트 등록해 즉시 무효화.
- `SecurityConfig`의 `authorizeHttpRequests`는 현재 `/api/v1/member/test`만 `authenticated()`, 나머지는 `permitAll` (개발 중 상태 — 실제 인가 규칙은 추후 정비 필요).
- 엔드포인트: `POST /api/v1/auth/{sign-in,reissue,logout}`, `POST /api/v1/member/sign-up`.
  - 주의: `MemberController`의 `@RequestMapping("/api/v1/member/")` + 메서드의 `@PostMapping("/sign-up")` 조합, `MovieController`의 `@RestController("/api/v1/movie")`(value 가 아니라 bean name 자리) 등 매핑에 정리가 덜 된 부분이 있다.

### 응답 / 예외

- **모든 컨트롤러는 `Result<T>`로 감싸 반환**한다: `{ code, message, data }`.
  - `code`는 HTTP 상태와 **별개**인 `ResponseCode` enum 값 (`SUCCESS(0)`, `DUPLICATE_EMAIL(1)`, `NO_MATCH_EMAIL_OR_PASSWORD(2)`, `INVALID_REFRESH_TOKEN(3)`, ... `EXCEPTION(99)`).
  - 성공: `Result.ofSuccess(data)`. 페이지네이션: `Result.ofPage` / `Result.ofSlice`.
- `GlobalExceptionAdvice`(`@RestControllerAdvice`)가 예외를 전부 잡아 `Result` 포맷 + 적절한 HTTP 상태로 변환한다. 새 도메인 예외는 여기에 핸들러를 추가한다.
- 일부 실패 케이스(예: `reissue`의 `InvalidRefreshTokenException`)는 예외를 던지지 않고 `HTTP 200 + 실패 code`로 응답한다 — 컨트롤러 코드를 확인할 것.
- 도메인 예외는 `support/exception/` 아래에 둔다 (`InvalidAccessTokenException`, `InvalidRefreshTokenException`).

### 엔티티 (`domain.entity`)

- **Soft delete**: 대부분 `@SQLRestriction("is_delete = 'N'")` + `@SQLDelete(... SET is_delete = 'Y' WHERE {pk} = ?)`. `repository.delete(...)` 호출 시 실제로는 UPDATE 되고, 조회는 자동으로 `is_delete='N'` 필터가 붙는다.
- **감사 필드**: `BaseEntity`(createdAt/updatedAt + createdBy/updatedBy + isDelete) 또는 `SimpleBaseEntity`(createdBy/updatedBy 없음) 상속. `@CreatedBy`/`@LastModifiedBy`를 채우려면 `AuditorAware<String>` 빈이 필요하다(현재 미등록 → createdBy 는 null).
- **생성자 패턴**: `@NoArgsConstructor(access = PROTECTED)` + 명시적 public 생성자 오버로딩. setter 지양(단 DTO `AddMemberDto` 등은 setter 사용).
- PK 컬럼명은 `{테이블}_id`, `@GeneratedValue(strategy = IDENTITY)`. 연관관계는 전부 `@ManyToOne(fetch = LAZY)`.
- **도메인 맵**:
  - `Member` — 등급 `MemberGrade`(ROLE_COMMON / ROLE_VIP / ROLE_MANAGE_ADMIN / ROLE_SUPER_ADMIN), 소셜 `SocialProvider`.
  - 상영: `Movie` → `Screening`(movie + theater + 시간 + price + `ScreeningStatus`) → `Theater` / `TheaterSeat`(좌석 등급 `SeatGrade`).
  - 예매: `MovieReservation`(member + screening + code + `ReservationStatus` + expiredAt/canceledAt) → `MovieReservationSeat`, 결제 `MoviePayment`.
  - 쿠폰: `Coupon` / `CouponIssuance`(발급 상태 `IssuanceStatus`).
  - `CommonCode` — 공통 코드 테이블.

### QueryDSL 커스텀 리포지토리

`Member*` 예시처럼 **3분할 패턴**을 쓴다:

- `MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryQuery` — Spring Data 진입점
- `MemberRepositoryQuery` — 커스텀 메서드 시그니처만 정의한 인터페이스
- `MemberRepositoryImpl implements MemberRepositoryQuery` — QueryDSL 동적 쿼리 구현 (네이밍 규칙상 `...Impl` 이어야 Spring Data 가 합쳐준다)

`Q*` 클래스는 `annotationProcessor`(`querydsl-apt ... :jpa`)가 컴파일 시 `build/generated/` 에 생성한다. 새 엔티티 추가 후 `./gradlew compileJava`.

### 인프라 의존

MySQL(Spring Data JDBC + JPA 둘 다 스타터 포함), Redis(토큰 저장), Kafka(`spring-kafka` 의존성만 추가된 상태 — 아직 사용처 없음), Spring Security.

### 주의사항
- docs/HISTORY.md 파일은 절대 수정하지 말 것.
