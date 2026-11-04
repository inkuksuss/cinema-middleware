#!/usr/bin/env node
// =============================================================================
// seat-race 회차·토큰 준비 스크립트 (Node.js 단독, 내장 fetch, 의존성 없음)
//
//   1. 이번 실행 회원(loadtest-<RUN_TAG>-NNNNNN) MEMBER_COUNT 명 로그인 → access 토큰을 TOKEN_FILE 에 저장(순서 = 회원 번호)
//      실패 회원이 1명이라도 있으면 중단(회원 1명당 1회 시도를 보장할 수 없음)
//   2. 관리자 API 로 상영관 SCREENING_COUNT 개 생성 → 관마다 좌석 SEATS 개(400개씩 나눠) 생성
//      → 관마다 ON_SALE 회차 1개(표식 상영일) 생성
//   진행 로그는 stderr, 마지막 stdout 에 JSON 한 줄만 출력한다.
//     {"screeningIds":[..],"theaterIds":[..],"movieId":N,"seatCount":N,
//      "tokenFile":"..","tokenCount":N,"tokenIssuedAt":<로그인 시작 epoch ms>}
//
// 사용법:
//   LOAD_TEST_PASSWORD=... TOKEN_FILE=/abs/tokens.json node scripts/load/seat-race/seed.js
//   (run.sh 가 호출하고 stdout 을 jq 로 파싱한다. 회원은 seed-members.sh 가 먼저 적재)
//
// 환경변수:
//   LOAD_TEST_PASSWORD(필수) TOKEN_FILE(필수, 절대 경로) RUN_TAG(필수) BASE_URL(http://localhost:8080)
//   MEMBER_COUNT(150000) LOGIN_CONCURRENCY(200) SCREENING_COUNT(3) SEATS(1000)
//   CINEMA_ID(1) MOVIE_ID(미지정 시 DISPLAY 영화 자동 선택)
//   LOADTEST_SCREENING_DATE(2099-12-31) LOADTEST_THEATER_PREFIX(LOADTEST-seat-race-)
//
// 관리자 API 는 현재 SecurityConfig 가 permitAll 이라 토큰 없이 호출한다.
// =============================================================================
'use strict';

const fs = require('node:fs');
const path = require('node:path');

// ----- 설정 (환경변수 + 기본값) -----
const BASE = process.env.BASE_URL || 'http://localhost:8080';
const PASSWORD = process.env.LOAD_TEST_PASSWORD || '';
const TOKEN_FILE = process.env.TOKEN_FILE || '';
const RUN_TAG = process.env.RUN_TAG || '';
const MEMBER_COUNT = process.env.MEMBER_COUNT || '150000';
const LOGIN_CONCURRENCY = process.env.LOGIN_CONCURRENCY || '200';
const SCREENING_COUNT = process.env.SCREENING_COUNT || '3';
const SEATS = process.env.SEATS || '1000';
const CINEMA_ID = process.env.CINEMA_ID || '1';
const MOVIE_ID = process.env.MOVIE_ID || '';
const SCREENING_DATE = process.env.LOADTEST_SCREENING_DATE || '2099-12-31';
const THEATER_PREFIX = process.env.LOADTEST_THEATER_PREFIX || 'LOADTEST-seat-race-';
const ADMIN = `${BASE}/api/v1/admin`;
// 요청 하나당 타임아웃(ms)
const TIMEOUT_MS = 30_000;
// 좌석 일괄 생성 API 1회 최대 개수(TheaterRequest.BulkCreateSeat @Size(max = 400))
const SEAT_BATCH = 400;
// 좌석 한 행의 열 수(A1~A20, B1~B20 ... Z, AA, AB ...)
const SEATS_PER_ROW = 20;
// 로그인 일시 오류(연결 실패, HTTP 503 = code 28 혼잡) 재시도 횟수와 간격(ms)
const LOGIN_RETRY = 3;
const RETRY_DELAY_MS = 500;
const CODE_SUCCESS = 200;
const CLEANUP_CMD = 'bash scripts/load/seat-race/reset.sh';

// ----- 로그(stderr 전용 — stdout 은 결과 JSON 한 줄만) -----
const info = (msg) => process.stderr.write(`\x1b[1;34m[info]\x1b[0m ${msg}\n`);
const warn = (msg) => process.stderr.write(`\x1b[1;33m[WARN]\x1b[0m ${msg}\n`);
const fail = (msg) => process.stderr.write(`\x1b[1;31m[FAIL]\x1b[0m ${msg}\n`);

// 양의 정수 문자열이면 숫자로, 아니면 null
const toPositiveInt = (v) => (/^[1-9][0-9]*$/.test(String(v)) ? Number(v) : null);
const sleepMs = (ms) => new Promise((resolve) => { setTimeout(resolve, ms); });

// ----- 입력값 검증 -----
function validate() {
  if (!/^https?:\/\/[A-Za-z0-9.:-]+$/.test(BASE)) throw new Error(`BASE_URL 형식 오류: ${BASE}`);
  if (!PASSWORD) throw new Error('LOAD_TEST_PASSWORD 를 지정하세요');
  if (!path.isAbsolute(TOKEN_FILE)) throw new Error('TOKEN_FILE 을 절대 경로로 지정하세요');
  // seed-members.sh 와 같은 규칙(소문자·숫자 1~20자)
  if (!/^[a-z0-9]{1,20}$/.test(RUN_TAG)) throw new Error(`RUN_TAG 형식 오류: ${RUN_TAG}`);
  for (const [name, v] of Object.entries({ CINEMA_ID, LOGIN_CONCURRENCY, SCREENING_COUNT })) {
    if (toPositiveInt(v) === null) throw new Error(`${name} 는 양의 정수여야 합니다: ${v}`);
  }
  // 이메일 번호는 6자리(loadtest-<RUN_TAG>-000001)
  const members = toPositiveInt(MEMBER_COUNT);
  if (members === null || members > 999_999) throw new Error(`MEMBER_COUNT 는 1~999999: ${MEMBER_COUNT}`);
  // k6 시나리오가 한 요청에 최대 4석을 고르므로 4석 이상이어야 한다
  const seats = toPositiveInt(SEATS);
  if (seats === null || seats < 4) throw new Error(`SEATS 는 4 이상의 정수여야 합니다: ${SEATS}`);
  if (MOVIE_ID && toPositiveInt(MOVIE_ID) === null) {
    throw new Error(`MOVIE_ID 는 양의 정수여야 합니다: ${MOVIE_ID}`);
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(SCREENING_DATE)) {
    throw new Error(`LOADTEST_SCREENING_DATE 형식 오류(YYYY-MM-DD): ${SCREENING_DATE}`);
  }
  if (!THEATER_PREFIX) throw new Error('LOADTEST_THEATER_PREFIX 가 비어 있습니다');
}

// ----- HTTP 요청 → { status, json } (연결 실패·타임아웃은 예외) -----
async function request(method, url, body) {
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
  // 응답 본문을 Result<T> 로 파싱(실패하면 null)
  let json = null;
  try {
    json = JSON.parse(text);
  } catch {
    json = null;
  }
  return { status: res.status, json };
}

// ----- 관리자 API 호출: HTTP 200 + Result.code === 200 이 아니면 예외, 성공 시 data 반환 -----
// 성공 code 는 Result.ofSuccess 의 HttpStatus.OK(200). 성공 시 message 는 null 일 수 있다
async function call(method, url, body) {
  const { status, json } = await request(method, url, body);
  const code = json && json.code !== undefined ? json.code : '?';
  if (status !== 200 || code !== CODE_SUCCESS) {
    const message = (json && json.message) || '';
    throw new Error(`${method} ${url} → HTTP ${status}, code ${code}, ${message}`);
  }
  return json.data;
}

// ----- 1. 회원 로그인 → 토큰 배열(인덱스 i = 회원 번호 i+1) -----
const memberEmail = (n) => `loadtest-${RUN_TAG}-${String(n).padStart(6, '0')}@example.com`;

// 회원 1명 로그인. 일시 오류만 재시도하고, 끝내 실패하면 사유 문자열을 던진다
async function signIn(email) {
  let reason = '';
  for (let attempt = 1; attempt <= LOGIN_RETRY; attempt++) {
    try {
      const { status, json } = await request('POST', `${BASE}/api/v1/auth/sign-in`,
        { email, password: PASSWORD });
      const accessToken = json && json.data && json.data.accessToken;
      if (status === 200 && json.code === CODE_SUCCESS && accessToken) return accessToken;
      reason = `HTTP ${status}, code ${json ? json.code : '?'}`;
      // 503(code 28 혼잡)만 재시도. 비밀번호 불일치 등은 바로 실패
      if (status !== 503) break;
    } catch (e) {
      reason = e.message;
    }
    await sleepMs(RETRY_DELAY_MS * attempt);
  }
  throw new Error(reason);
}

// LOGIN_CONCURRENCY 개 작업자가 다음 회원 번호를 가져가며 로그인한다
async function loginAll(count, concurrency) {
  const tokens = new Array(count);
  const failed = [];
  let next = 0;
  let done = 0;
  const started = Date.now();
  const worker = async () => {
    while (next < count) {
      const i = next++;
      try {
        tokens[i] = await signIn(memberEmail(i + 1));
      } catch (e) {
        failed.push(`${memberEmail(i + 1)}: ${e.message}`);
      }
      done++;
      // 1만 명마다 진행 상황 출력
      if (done % 10_000 === 0) info(`로그인 ${done}/${count} (${Math.round((Date.now() - started) / 1000)}초)`);
    }
  };
  await Promise.all(Array.from({ length: Math.min(concurrency, count) }, worker));
  return { tokens, failed, elapsedSec: Math.round((Date.now() - started) / 1000) };
}

// ----- 2. 상영 가능(DISPLAY) 영화 id 선택. MOVIE_ID 가 있으면 그대로 쓴다 -----
async function pickMovie() {
  if (MOVIE_ID) return Number(MOVIE_ID);
  const data = await call('GET', `${ADMIN}/movie/?status=DISPLAY&size=1`);
  const id = data && data.content && data.content[0] && data.content[0].id;
  if (toPositiveInt(id) === null) throw new Error('DISPLAY 상태 영화가 없습니다(MOVIE_ID 로 지정)');
  return Number(id);
}

// ----- 3. 상영관 생성 후 id 확보 -----
// 이름: 표식 접두어 + 시각(YYYYMMDDhhmmss) + PID + 순번으로 영화관 내 이름 중복을 피한다
function theaterName(no) {
  const d = new Date();
  const p = (v) => String(v).padStart(2, '0');
  const ts = `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}`
    + `${p(d.getHours())}${p(d.getMinutes())}${p(d.getSeconds())}`;
  return `${THEATER_PREFIX}${ts}-${process.pid}-${no}`;
}

async function createTheater(cinemaId, no) {
  const name = theaterName(no);
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

// ----- 4. 좌석 생성: 20열씩 A..Z, AA.. 행, 모두 NORMAL 8000원. 400개씩 나눠 호출 -----
// 행 번호(0부터) → 엑셀식 행 이름(0=A, 25=Z, 26=AA)
function rowName(idx) {
  let n = idx + 1;
  let s = '';
  while (n > 0) {
    s = String.fromCharCode(65 + ((n - 1) % 26)) + s;
    n = Math.floor((n - 1) / 26);
  }
  return s;
}

async function createSeats(theaterId, total) {
  for (let from = 0; from < total; from += SEAT_BATCH) {
    // 전역 순번 i 로 행·열을 정해 호출 간 좌석 위치가 겹치지 않게 한다
    const seatList = [];
    for (let i = from; i < Math.min(from + SEAT_BATCH, total); i++) {
      seatList.push({
        seatRow: rowName(Math.floor(i / SEATS_PER_ROW)),
        seatColumn: String((i % SEATS_PER_ROW) + 1),
        grade: 'NORMAL',
        price: 8000,
        status: 'AVAILABLE',
      });
    }
    await call('POST', `${ADMIN}/theater/${theaterId}/seat`, { seatList });
  }
  info(`상영관 ${theaterId}: 좌석 ${total}개 생성`);
}

// ----- 5. 회차 생성(표식 상영일 + ON_SALE) 후 id 확보 -----
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
  info(`회차 생성: screening_id=${ids[0]} (theater=${theaterId}, movie=${movieId})`);
  return Number(ids[0]);
}

async function main() {
  validate();
  const memberCount = Number(MEMBER_COUNT);
  const cinemaId = Number(CINEMA_ID);
  const seatCount = Number(SEATS);

  // 1) 로그인을 먼저 한다 → 실패해도 남는 상영관이 없다. 토큰 수명은 로그인 시작부터 센다
  const tokenIssuedAt = Date.now();
  info(`회원 ${memberCount}명 로그인 시작(동시 ${LOGIN_CONCURRENCY})`);
  const { tokens, failed, elapsedSec } = await loginAll(memberCount, Number(LOGIN_CONCURRENCY));
  if (failed.length > 0) {
    failed.slice(0, 5).forEach((f) => warn(`  ${f}`));
    throw new Error(`로그인 실패 ${failed.length}명 — 1인 1회를 보장할 수 없어 중단`);
  }
  info(`로그인 완료 ${memberCount}명, ${elapsedSec}초`);
  // 토큰 파일은 소유자만 읽게(0600) 쓰고, run.sh 가 종료 시 지운다
  fs.mkdirSync(path.dirname(TOKEN_FILE), { recursive: true });
  fs.writeFileSync(TOKEN_FILE, JSON.stringify(tokens), { mode: 0o600 });

  // 2) 상영관·좌석·회차 생성. 상영관 생성 이후 실패하면 정리 명령을 안내한다
  const movieId = await pickMovie();
  const theaterIds = [];
  const screeningIds = [];
  try {
    for (let no = 1; no <= Number(SCREENING_COUNT); no++) {
      const theaterId = await createTheater(cinemaId, no);
      theaterIds.push(theaterId);
      await createSeats(theaterId, seatCount);
      screeningIds.push(await createScreening(movieId, theaterId));
    }
  } catch (e) {
    fail(e.message);
    if (theaterIds.length > 0) warn(`준비 중 중단 — run.sh 가 종료 시 정리(단독 실행이면: ${CLEANUP_CMD})`);
    process.exit(1);
  }

  // 결과 JSON 한 줄을 stdout 으로(run.sh 가 jq 로 파싱)
  const result = {
    screeningIds, theaterIds, movieId, seatCount,
    tokenFile: TOKEN_FILE, tokenCount: tokens.length, tokenIssuedAt,
  };
  process.stdout.write(`${JSON.stringify(result)}\n`);
}

// 상영관 생성 전 실패(입력 오류·로그인 실패 등)는 남는 데이터가 없으므로 사유만 출력
main().catch((e) => {
  fail(e.message);
  process.exit(1);
});
