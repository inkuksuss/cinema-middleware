# 인증 (JWT, 무상태)

## 구성

- `SecurityConfig`: `STATELESS`, CSRF·formLogin·httpBasic 비활성. `JwtAuthenticationFilter` 를
  `UsernamePasswordAuthenticationFilter` 앞에 둔다. 인증 실패는 `JwtAuthenticationEntryPoint` 가
  401 + `Result(UNAUTHORIZED)` JSON 으로 응답한다. 인가 규칙은 `filterChain` 의 `authorizeHttpRequests` 에 있다.
- `JwtAuthenticationFilter`: `Authorization: Bearer <token>` 이 유효하고 블랙리스트에 없으면
  `SecurityContext` 에 인증을 넣는다. **principal 은 `Long memberId`**(엔티티 아님), authority 는 토큰의 `role` 클레임 하나.
- `JwtTokenProvider`: jjwt 0.11.5, HS256. `jwt.secret` 을 BASE64 디코드해 키로 쓴다.
  subject = memberId, 클레임 `email`·`role`. 만료·서명 오류는 모두 무효(`isValid` false).

## 서비스에서 현재 회원 얻기 (`AuthService`)

- `getMemberId()` — SecurityContext 의 principal. 인증이 없으면 `AuthenticationFailedException`.
- `getMember()` — 회원 조회 + 고객 등급(ROLE_COMMON/ROLE_VIP)이 아니면 `AccessDeniedException`.
- `getMemberRefer()` — 프록시 참조.

## 로그인 · 재발급 · 로그아웃

- `login`: `AuthenticationManager` → `UserDetailsHandler`(이메일로 `Member` 조회 → `MemberPrincipal`)
  + `BCryptPasswordEncoder` 로 검증 후 access/refresh 토큰을 발급하고 refresh 를 Redis 에 저장한다.
- `reissue`: 저장된 refresh 토큰과 정확히 일치해야 재발급한다(재사용 방지).
  실패 시 컨트롤러가 **HTTP 200 + `INVALID_REFRESH_TOKEN`** 으로 응답한다.
- `logout`: refresh 키 삭제 + access 토큰을 남은 TTL 동안 블랙리스트에 등록한다.

## Redis 키 (`AuthTokenRepository` 에만 존재 — 포맷 변경은 이 클래스 한 곳)

| 키 | 값 |
| --- | --- |
| `refresh:{memberId}` | refresh 토큰 |
| `blacklist:{accessToken}` | `"logout"` (남은 만료 시간만큼 TTL) |

## 엔드포인트

`POST /api/v1/auth/sign-in`, `/reissue`, `/logout`, `POST /api/v1/member/sign-up`, `POST /api/v1/member/me`.
`logout` 과 `member/me` 는 `@RequestHeader("Authorization")` 필수다(`member/me` 는 헤더의 토큰을 직접 파싱한다).
