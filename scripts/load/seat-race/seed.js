#!/usr/bin/env node
// =============================================================================
// seat-race 회차 준비 스크립트 (Node.js 단독, 내장 fetch, 의존성 없음)
//
//   관리자 API 로 상영관 생성 → 좌석 EXPECTED_SEATS 개 일괄 생성 → ON_SALE 회차 생성(표식 상영일)
//   진행 로그는 stderr, 마지막 stdout 에 JSON 한 줄만 출력한다.
//     {"screeningId":N,"theaterId":N,"movieId":N,"seatCount":N}
//
// 사용법:
//   node scripts/load/seat-race/seed.js
//   EXPECTED_SEATS=100 MOVIE_ID=3 node scripts/load/seat-race/seed.js
//   (run.sh 는 SCREENING_ID 미지정 시 이 스크립트를 호출하고 stdout 을 jq 로 파싱한다)
//
// 환경변수:
//   BASE_URL(http://localhost:8080) CINEMA_ID(1) MOVIE_ID(미지정 시 DISPLAY 영화 자동 선택)
//   EXPECTED_SEATS(300, 1~400) LOADTEST_SCREENING_DATE(2099-12-31)
//   LOADTEST_THEATER_PREFIX(LOADTEST-seat-race-) — run.sh 가 common.sh 의 표식 값을 넘긴다
//
// 관리자 API 는 현재 SecurityConfig 가 permitAll 이라 토큰 없이 호출한다.
// =============================================================================
'use strict';

// ----- 설정 (환경변수 + 기본값) -----
const BASE = process.env.BASE_URL || 'http://localhost:8080';
const CINEMA_ID = process.env.CINEMA_ID || '1';
const MOVIE_ID = process.env.MOVIE_ID || '';
const EXPECTED_SEATS = process.env.EXPECTED_SEATS || '300';
const SCREENING_DATE = process.env.LOADTEST_SCREENING_DATE || '2099-12-31';
const THEATER_PREFIX = process.env.LOADTEST_THEATER_PREFIX || 'LOADTEST-seat-race-';
const ADMIN = `${BASE}/api/v1/admin`;
// 요청 하나당 타임아웃(ms)
const TIMEOUT_MS = 30_000;
// 좌석 한 행의 열 수(A1~A20, B1~B20 ...)
const SEATS_PER_ROW = 20;
const CLEANUP_CMD = 'bash scripts/load/seat-race/cleanup.sh --theater';

// ----- 로그(stderr 전용 — stdout 은 결과 JSON 한 줄만) -----
const info = (msg) => process.stderr.write(`\x1b[1;34m[info]\x1b[0m ${msg}\n`);
const warn = (msg) => process.stderr.write(`\x1b[1;33m[WARN]\x1b[0m ${msg}\n`);
const fail = (msg) => process.stderr.write(`\x1b[1;31m[FAIL]\x1b[0m ${msg}\n`);

// 양의 정수 문자열이면 숫자로, 아니면 null
const toPositiveInt = (v) => (/^[1-9][0-9]*$/.test(String(v)) ? Number(v) : null);

// ----- 입력값 검증 -----
function validate() {
  if (!/^https?:\/\/[A-Za-z0-9.:-]+$/.test(BASE)) throw new Error(`BASE_URL 형식 오류: ${BASE}`);
  if (toPositiveInt(CINEMA_ID) === null) throw new Error(`CINEMA_ID 는 양의 정수여야 합니다: ${CINEMA_ID}`);
  if (MOVIE_ID && toPositiveInt(MOVIE_ID) === null) {
    throw new Error(`MOVIE_ID 는 양의 정수여야 합니다: ${MOVIE_ID}`);
  }
  // 좌석 일괄 생성 API 는 한 번에 최대 400개(TheaterRequest.BulkCreateSeat @Size)
  const n = toPositiveInt(EXPECTED_SEATS);
  if (n === null || n > 400) throw new Error(`EXPECTED_SEATS 는 1~400 이어야 합니다: ${EXPECTED_SEATS}`);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(SCREENING_DATE)) {
    throw new Error(`LOADTEST_SCREENING_DATE 형식 오류(YYYY-MM-DD): ${SCREENING_DATE}`);
  }
  if (!THEATER_PREFIX) throw new Error('LOADTEST_THEATER_PREFIX 가 비어 있습니다');
}

// ----- API 호출: HTTP 200 + Result.code === 200 이 아니면 예외, 성공 시 Result.data 반환 -----
// 성공 code 는 Result.ofSuccess 의 HttpStatus.OK(200). 성공 시 message 는 null 일 수 있다
async function call(method, url, body) {
  const init = { method, signal: AbortSignal.timeout(TIMEOUT_MS) };
  // 본문이 있으면 JSON 으로 보낸다
  if (body !== undefined) {
    init.headers = { 'Content-Type': 'application/json' };
    init.body = JSON.stringify(body);
  }
  let res;
  try {
    res = await fetch(url, init);
  } catch (e) {
    // 연결 실패·타임아웃: 원인(cause)까지 보여 준다
    const cause = e.cause ? ` (${e.cause.code || e.cause.message})` : '';
    throw new Error(`${method} ${url} → 요청 실패: ${e.name}: ${e.message}${cause}`);
  }
  const text = await res.text();
  // 응답 본문을 Result<T> 로 파싱(실패해도 HTTP 상태와 함께 보고)
  let json = null;
  try {
    json = JSON.parse(text);
  } catch {
    json = null;
  }
  const code = json && json.code !== undefined ? json.code : '?';
  if (res.status !== 200 || code !== 200) {
    const message = (json && json.message) || '';
    throw new Error(`${method} ${url} → HTTP ${res.status}, code ${code}, ${message}`);
  }
  return json.data;
}

// ----- 1. 상영 가능(DISPLAY) 영화 id 선택. MOVIE_ID 가 있으면 그대로 쓴다 -----
async function pickMovie() {
  if (MOVIE_ID) return Number(MOVIE_ID);
  const data = await call('GET', `${ADMIN}/movie/?status=DISPLAY&size=1`);
  const id = data && data.content && data.content[0] && data.content[0].id;
  if (toPositiveInt(id) === null) throw new Error('DISPLAY 상태 영화가 없습니다(MOVIE_ID 로 지정)');
  return Number(id);
}

// ----- 2. 상영관 생성 후 id 확보 -----
// 이름: 표식 접두어 + 시각(YYYYMMDDhhmmss) + PID 로 영화관 내 이름 중복을 피한다
function theaterName() {
  const d = new Date();
  const p = (v) => String(v).padStart(2, '0');
  const ts = `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}`
    + `${p(d.getHours())}${p(d.getMinutes())}${p(d.getSeconds())}`;
  return `${THEATER_PREFIX}${ts}-${process.pid}`;
}

async function createTheater(cinemaId) {
  const name = theaterName();
  await call('POST', `${ADMIN}/theater/`, {
    cinemaId,
    name,
    description: 'seat-race 부하 테스트용(자동 생성)',
  });
  // 생성 응답에 id 가 없어 목록(생성일 내림차순)에서 이름으로 찾는다
  const data = await call('GET', `${ADMIN}/theater/?cinemaId=${cinemaId}&size=50`);
  const found = ((data && data.content) || []).find((t) => t.name === name);
  if (!found || toPositiveInt(found.id) === null) {
    throw new Error(`생성한 상영관을 목록에서 찾지 못했습니다: ${name}`);
  }
  info(`상영관 생성: theater_id=${found.id} (${name}, cinema=${cinemaId})`);
  return Number(found.id);
}

// ----- 3. 좌석 n 개 일괄 생성: 20열씩 A, B, C... 행, 모두 NORMAL 8000원 -----
function seatPayload(n) {
  const seatList = Array.from({ length: n }, (_, i) => ({
    seatRow: String.fromCharCode(65 + Math.floor(i / SEATS_PER_ROW)),
    seatColumn: String((i % SEATS_PER_ROW) + 1),
    grade: 'NORMAL',
    price: 8000,
    status: 'AVAILABLE',
  }));
  return { seatList };
}

// ----- 4. 회차 생성(표식 상영일 + ON_SALE) 후 id 확보 -----
async function createScreening(movieId, theaterId) {
  await call('POST', `${ADMIN}/screening/`, {
    movieId,
    theaterId,
    screenType: 'V_2D',
    startAt: `${SCREENING_DATE}T10:00:00`,
    status: 'ON_SALE',
  });
  // 생성 응답에 id 가 없어 표식 상영일 + 새 상영관 조건으로 찾는다(새 관이라 1건이어야 함)
  const q = `targetDate=${SCREENING_DATE}&theaterId=${theaterId}&size=10`;
  const data = await call('GET', `${ADMIN}/screening/?${q}`);
  const ids = ((data && data.content) || []).map((s) => s.id);
  if (ids.length !== 1 || toPositiveInt(ids[0]) === null) {
    throw new Error(`생성한 회차를 1건으로 특정하지 못했습니다: ${ids.join(',') || '없음'}`);
  }
  return Number(ids[0]);
}

async function main() {
  validate();
  const cinemaId = Number(CINEMA_ID);
  const seatCount = Number(EXPECTED_SEATS);
  const movieId = await pickMovie();
  const theaterId = await createTheater(cinemaId);

  // 상영관 생성 이후 실패하면 상영관이 남으므로 정리 명령을 안내하고 중단한다
  try {
    await call('POST', `${ADMIN}/theater/${theaterId}/seat`, seatPayload(seatCount));
    info(`좌석 ${seatCount}개 일괄 생성`);
    const screeningId = await createScreening(movieId, theaterId);
    info(`새 회차 생성: screening_id=${screeningId} (theater=${theaterId}, movie=${movieId})`);
    // 결과 JSON 한 줄을 stdout 으로(run.sh 가 jq 로 파싱)
    process.stdout.write(`${JSON.stringify({ screeningId, theaterId, movieId, seatCount })}\n`);
  } catch (e) {
    fail(e.message);
    warn(`준비 중 중단 — 정리: ${CLEANUP_CMD} ${theaterId}`);
    process.exit(1);
  }
}

// 상영관 생성 전 실패(입력 오류·연결 실패 등)는 남는 데이터가 없으므로 사유만 출력
main().catch((e) => {
  fail(e.message);
  process.exit(1);
});
