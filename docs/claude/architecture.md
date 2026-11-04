# 패키지 · 계층 · 응답/예외

## 패키지 (`com.example.cinema_middleware.v1.*`)

| 패키지 | 내용 |
| --- | --- |
| `controller` | REST 컨트롤러. `request/`(입력 record), `response/`(`Result`, `ResponseCode`) |
| `service` | 비즈니스 로직. `dto/`(`XxxCommand`·`XxxDto`), `dto/forms/`, `factory/`(`XxxDtoFactory`) |
| `repository` | Spring Data + QueryDSL. `dao/`(조회 projection record), `dao/search/`(검색 조건) |
| `domain.entity` | JPA 엔티티. `base/`, `enums/`, `key/`(복합키) → domain.md |
| `security` | JWT 인증 → auth.md |
| `config` | `SecurityConfig`, `WebConfig` |
| `support` | `GlobalExceptionAdvice`, `QueryDslUtils`, `exception/`, `converter/` |

관리자 기능은 `Admin*Controller`(`/api/v1/admin/**`) → `Admin*Service` 로 분리되어 있다.

## 요청 흐름

1. 컨트롤러가 `XxxRequest.*` record 를 `@Validated` 로 받는다. 대부분 `toCommand()` 로 서비스 입력
   `XxxCommand.*` record 를 만든다(예매·인증 요청은 필드를 직접 넘긴다).
2. 서비스는 엔티티 또는 `repository/dao` 의 projection record(`XxxDao.*`)를 조회한다.
3. 응답 DTO(`XxxDto.*` record, 평탄화 구조)는 `XxxDtoFactory` 의 static 메서드로 만든다.
   연관 엔티티가 null 일 수 있는 매핑은 `FactorySupport.nullSafe` 를 쓴다.
4. 컨트롤러는 `ResponseEntity<Result<T>>` 로 감싸 반환한다.

페이징: `Pageable` 을 받아 `Page`/`Slice` 반환. Slice 생성·정렬 변환은 `QueryDslUtils.createSlice` / `getSort`.

## 응답 포맷 (`Result<T>`)

- `{ code, message, data }`. 성공은 `Result.ofSuccess(data)`, 페이지는 `Result.ofPage` / `Result.ofSlice`.
- 실패 `code` 는 `ResponseCode` enum 값이다(`controller/response/ResponseCode.java`). HTTP 상태와 별개다.
- 예매·인증 등 일부 컨트롤러는 서비스 예외를 직접 catch 해 **HTTP 200 + 실패 code** 로 응답한다
  (`MovieReservationController`, `AuthController#reissue`, `AdminScreeningController`, `AdminTheaterController`).

## 예외

- 도메인 예외는 `support/exception/` 에 둔다(전부 `RuntimeException`).
- `GlobalExceptionAdvice` 가 예외를 `Result` + HTTP 상태로 변환한다. 새 예외는 여기에 핸들러를 추가한다.

| 예외 | HTTP | code |
| --- | --- | --- |
| `AccessDeniedException`, `InvalidAccessTokenException` | 401 | `UNAUTHORIZED` |
| `BadCredentialsException` | 401 | `NO_MATCH_EMAIL_OR_PASSWORD` |
| `AuthenticationException` | 401 | `ILLEGAL_STATE` |
| `TargetNotFoundException` | 404 | `NOT_FOUND` |
| `NoResourceFoundException`(매핑 없는 경로, 스택 없이 WARN 로그) | 404 | `NOT_FOUND` |
| `DuplicateValueException` | 409 | `DUPLICATE_VALUE` |
| `ObjectNotFoundException` | 400 | `NOT_FOUND` |
| `FileSaveFailedException` | 400 | `INVALID_FILE` |
| 검증·파라미터 오류, `IllegalArgumentException` | 400 | `INVALID_ARGUMENT` |
| `ConstraintViolationException`, `DataIntegrityViolationException` | 409 | `TRANSACTION_FAILED` |
| 풀 대기·쿼리·트랜잭션·Redis 시간 초과, DB·Redis 연결 실패, 잠금 대기 초과(`CannotCreateTransactionException`, `QueryTimeoutException`, `TransactionTimedOutException`, `DataAccessResourceFailureException`, `PessimisticLockingFailureException`) | 503 | `SERVICE_UNAVAILABLE` |
| 그 외 `Exception` | 500 | `EXCEPTION` |

- `JwtAuthenticationFilter` 는 MVC 밖이라 Advice 가 적용되지 않는다. Redis 블랙리스트 조회 실패 시 필터가 직접 503 + `SERVICE_UNAVAILABLE` 을 쓴다.

## 파일

`FileService` 구현은 `LocalFileService`(`@Primary`, 실사용)와 `S3FileService`(미구현 골격) 두 개다.
파일 메타데이터는 `file_master`(`FileMaster`)에 `target_type` + `target_id` 로 저장한다.

## 엔드포인트

| 컨트롤러 | 경로 |
| --- | --- |
| `AuthController` | `POST /api/v1/auth/{sign-in, reissue, logout}` |
| `MemberController` | `POST /api/v1/member/sign-up`, `POST /api/v1/member/me` |
| `MovieController` | `GET /api/v1/movie/`, `GET /api/v1/movie/{movieId}/detail` |
| `ScreeningController` | `GET /api/v1/screening/`, `GET /api/v1/screening/{screeningId}/seat` |
| `MovieReservationController` | `POST /api/v1/reservation/`, `POST /{id}/confirm`, `GET /my`, `GET /{id}/detail` |
| `AdminMovieController` | `/api/v1/admin/movie` — `GET /`, `GET·PUT·DELETE /{movieId}`, `POST /`(multipart) |
| `AdminScreeningController` | `/api/v1/admin/screening` — `GET /`, `GET·PATCH·DELETE /{screeningId}` |
| `AdminTheaterController` | `/api/v1/admin/theater` — `GET·POST /`, `PUT·DELETE /{theaterId}`, `GET·POST /{theaterId}/seat`, `DELETE /{seatId}/seat` |
| `AdminMovieReservationController` | `/api/v1/admin/reservation` — `GET /`, `GET /{id}`, `POST /{id}/cancel` |
| (actuator) | `GET /actuator/health` — UP 200 / DOWN 503, 상세 비노출 |
