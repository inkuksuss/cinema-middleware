// =============================================================================
// 좌석 선점 경쟁 부하 테스트 (POST /api/v1/reservation/)
//
// 목적: 좌석 300개짜리 회차 하나에 정확히 50,000건 예매를 초당 최대 5000건(약 10초)으로 몰아
//       uk_seat_hold 가 중복 점유를 막는지(정확히 300석만 RESERVED), 나머지 요청이
//       모두 SEAT_ALREADY_TAKEN(13) 으로 끝나는지 확인한다.
//       (300석 판정은 부하 후 verify.sh 가 좌석 배치 API 로 한다)
// 측정: 응답 code 별 건수, 보낸 요청 수, 성공 요청이 차지한 좌석 수, 결과별 응답시간.
//
// 부하 모델: per-vu-iterations (VUS 명 x TOTAL/VUS 회 = 정확히 TOTAL 건, 누락 없음)
//   - 반복 번호 i = (VU 의 반복 순번) x VUS + (VU 번호 - 1) → 0 ~ TOTAL-1 이 한 번씩.
//   - 반복 i 는 시작 시각 + i / RATE 초 이전에는 시작하지 않는다(도착률 RATE/s 로 분산).
//   - 각 VU 는 직전 시작 후 VUS / RATE 초(기본 1초)가 지나야 다음을 시작한다
//     → 전체 시작 수는 어느 1초 구간에서도 RATE 를 넘지 않는다(늦어져도 몰아서 보내지 않음).
//   - 서버가 느리면 총 소요 시간이 10초보다 길어질 뿐 요청 수는 줄지 않는다(MAX_DURATION 까지).
//
// 요청 설계
//   - 반복 번호 i 로 좌석 수 k = (i % 4) + 1 을 정한다 → 1~4석이 1/4 씩.
//   - k=1 요청은 좌석 목록을 순서대로 돈다 → 처음 4 x 좌석수(1200) 회 안에 모든 좌석이
//     1석 요청을 한 번씩 받는다(남은 좌석이 조합과 안 맞아 비는 상황을 막는 장치).
//   - k=2~4 요청은 임의 시작점에서 연속 k석(목록 끝에서 처음으로 이어짐) → 좌석별 요청 수 균일.
//   - 한 요청 안에서 좌석은 겹치지 않는다(DUPLICATE_SEAT 방지).
//
// 실행 예 (보통은 run.sh 가 SCREENING_ID·SEAT_IDS 를 채워 실행한다)
//   LOAD_TEST_PASSWORD=... SCREENING_ID=1051 SEAT_IDS=1,2,...,300 \
//     k6 run --summary-export build/load-test/summary.json scripts/load/seat-race/seat-race.js
// =============================================================================
import http from 'k6/http';
import exec from 'k6/execution';
import { check, fail, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// ----- 설정 (환경변수 + 기본값) -----
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SCREENING_ID = Number(__ENV.SCREENING_ID);
// 대상 회차의 예약 가능 좌석 id 목록(쉼표 구분). run.sh 가 좌석 배치 API 로 조회해 넘긴다
const SEAT_IDS = (__ENV.SEAT_IDS || '').split(',').filter((s) => s !== '').map(Number);
const TOTAL = Number(__ENV.TOTAL || 50000); // 전체 참여자(요청) 수 — 정확히 이만큼 보낸다
const RATE = Number(__ENV.RATE || 5000); // 초당 최대 시작 요청 수
const VUS = Number(__ENV.VUS || 5000); // 동시 사용자(VU) 수. TOTAL 의 약수여야 한다
const ITERS_PER_VU = TOTAL / VUS; // VU 1명이 보내는 요청 수(기본 10)
const VU_INTERVAL_MS = (VUS / RATE) * 1000; // VU 1명의 최소 요청 간격(기본 1초)
// 모든 요청이 끝나야 하는 상한. 넘으면 남은 반복이 중단되어 요청 수가 TOTAL 에 못 미친다
const MAX_DURATION = __ENV.MAX_DURATION || '10m';
const REQ_TIMEOUT = __ENV.REQ_TIMEOUT || '30s'; // 요청 1건 타임아웃
// 토큰을 받을 회원: member{USER_START..USER_START+USER_COUNT-1}@example.com
const USER_COUNT = Number(__ENV.USER_COUNT || 100);
const USER_START = Number(__ENV.USER_START || 5);
const LOGIN_BATCH = Number(__ENV.LOGIN_BATCH || 20); // setup 로그인 동시 요청 수
const PASSWORD = __ENV.LOAD_TEST_PASSWORD;

// ----- 응답 code: 성공은 Result.ofSuccess 의 HttpStatus.OK(200), 실패는 ResponseCode 값 -----
const CODE_SUCCESS = 200;
const CODE_SEAT_ALREADY_TAKEN = 13;
// 성공 응답 data(예매 PK, UUID v7) 형식
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
// 예상 밖 code 를 이름별로 세기 위한 표 (ResponseCode.java 와 동일)
const CODE_NAMES = {
  1: 'DUPLICATE_EMAIL', 2: 'NO_MATCH_EMAIL_OR_PASSWORD', 3: 'INVALID_REFRESH_TOKEN',
  4: 'INVALID_FILE', 5: 'INVALID_ARGUMENT', 6: 'ILLEGAL_STATE', 7: 'NO_SUPPORT_METHOD',
  8: 'NO_SUPPORT_CONTENT_TYPE', 9: 'UNAUTHORIZED', 10: 'FORBIDDEN', 11: 'NOT_FOUND',
  12: 'PAYMENT_FAILED', 14: 'SCREENING_NOT_ON_SALE', 15: 'EXCEED_SEAT_LIMIT',
  16: 'RESERVATION_EXPIRED', 17: 'SCREENING_CANT_UPDATE', 18: 'SCREENING_TIME_CONFLICT',
  19: 'SCREENING_IN_USE', 20: 'MOVIE_IN_USE', 21: 'THEATER_IN_USE', 22: 'SEAT_IN_USE',
  23: 'DUPLICATE_SEAT', 24: 'DUPLICATE_VALUE', 25: 'SEAT_NOT_ON_SALE',
  26: 'TRANSACTION_FAILED', 27: 'RESERVED_SEAT_INCLUDE', 99: 'EXCEPTION',
};

// ----- init 단계 입력 검증 (잘못된 설정이면 부하 전에 중단) -----
if (!Number.isInteger(SCREENING_ID) || SCREENING_ID <= 0) {
  throw new Error('SCREENING_ID 를 양의 정수로 지정하세요');
}
if (SEAT_IDS.length < 4 || SEAT_IDS.some((id) => !Number.isInteger(id) || id <= 0)) {
  throw new Error('SEAT_IDS 는 4개 이상의 양의 정수(쉼표 구분)여야 합니다');
}
if (new Set(SEAT_IDS).size !== SEAT_IDS.length) {
  throw new Error('SEAT_IDS 에 중복 좌석이 있습니다');
}
if (![TOTAL, RATE, VUS].every((v) => Number.isInteger(v) && v > 0)) {
  throw new Error('TOTAL·RATE·VUS 는 양의 정수여야 합니다');
}
if (TOTAL % VUS !== 0) {
  throw new Error(`TOTAL(${TOTAL}) 이 VUS(${VUS}) 로 나누어떨어져야 합니다`);
}
if (!PASSWORD) {
  throw new Error('LOAD_TEST_PASSWORD 를 지정하세요');
}

// ----- 사용자 지표 -----
const reserveSuccess = new Counter('reserve_success'); // code 200 (선점 성공) 응답 수
const reserveSeatTaken = new Counter('reserve_seat_taken'); // code 13 응답 수
// 성공·13 외 모든 결과(다른 code, HTTP 오류·타임아웃, 파싱 실패). 0 이어야 기준 2 통과
const reserveUnexpected = new Counter('reserve_unexpected');
const reserveSeatsWon = new Counter('reserve_seats_won'); // 성공 요청이 차지한 좌석 수 합
const reserveHttpError = new Counter('reserve_http_error'); // HTTP 200 이 아닌 응답(0=타임아웃 등)
const reserveParseError = new Counter('reserve_parse_error'); // 본문 JSON/code 파싱 실패
const durationSuccess = new Trend('reserve_duration_success', true); // 성공 응답시간
const durationTaken = new Trend('reserve_duration_seat_taken', true); // 13 응답시간
// 예상 밖 code 별 카운터(값이 생긴 것만 요약에 표시됨)
const unexpectedByCode = {};
for (const [code, name] of Object.entries(CODE_NAMES)) {
  unexpectedByCode[code] = new Counter(`reserve_code_${code}_${name.toLowerCase()}`);
}
const unexpectedUnknownCode = new Counter('reserve_code_unknown'); // 표에 없는 code

const reserveRequests = new Counter('reserve_requests'); // 실제로 보낸 예매 요청 수

// ----- 부하 모델: VUS 명이 TOTAL/VUS 회씩 = 정확히 TOTAL 건 (도착률 제한은 default 함수) -----
export const options = {
  scenarios: {
    seat_race: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: ITERS_PER_VU,
      maxDuration: MAX_DURATION,
    },
  },
  setupTimeout: '180s', // 회원 로그인(BCrypt) 시간이 길 수 있어 기본 60초보다 늘린다
  // 합격 기준을 임계값으로 표현 (실패 시 k6 종료 코드 99). 300석 판정은 verify.sh(API)
  thresholds: {
    reserve_unexpected: ['count<1'], // 기준 2: 성공·SEAT_ALREADY_TAKEN 외 결과 0건
    reserve_requests: [`count==${TOTAL}`], // 보낸 요청 수 = TOTAL(중단된 반복이 없어야 함)
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// ----- setup: 회원 USER_COUNT 명 로그인 → 토큰 목록을 VU 들이 나눠 쓴다 -----
export function setup() {
  const url = `${BASE_URL}/api/v1/auth/sign-in`;
  const tokens = [];
  const failed = [];
  // LOGIN_BATCH 명씩 묶어 병렬 로그인
  for (let start = 0; start < USER_COUNT; start += LOGIN_BATCH) {
    const emails = [];
    for (let n = start; n < Math.min(start + LOGIN_BATCH, USER_COUNT); n++) {
      emails.push(`member${String(USER_START + n).padStart(5, '0')}@example.com`);
    }
    const responses = http.batch(emails.map((email) => ({
      method: 'POST',
      url,
      body: JSON.stringify({ email, password: PASSWORD }),
      params: { headers: { 'Content-Type': 'application/json' }, tags: { name: 'sign-in' } },
    })));
    // 성공(code 200 + accessToken)만 토큰 목록에 담고, 실패(탈퇴 회원 등)는 이메일만 모은다
    responses.forEach((res, idx) => {
      let token = null;
      try {
        if (res.status === 200 && res.json('code') === CODE_SUCCESS) token = res.json('data.accessToken');
      } catch (e) {
        token = null;
      }
      if (token) tokens.push(token);
      else failed.push(emails[idx]);
    });
  }
  if (failed.length > 0) {
    console.warn(`로그인 실패 ${failed.length}명(탈퇴 회원 등, 제외하고 진행): ${failed.slice(0, 10).join(', ')}`);
  }
  if (tokens.length === 0) {
    fail('로그인에 성공한 회원이 없습니다. LOAD_TEST_PASSWORD 와 테스트 데이터를 확인하세요');
  }
  console.log(`토큰 ${tokens.length}개 준비, 좌석 ${SEAT_IDS.length}개, 회차 ${SCREENING_ID}`);
  return { tokens };
}

// 반복 번호 i 로 요청 좌석 목록을 만든다(설계는 파일 상단 주석 참고)
function pickSeats(i) {
  const n = SEAT_IDS.length;
  const k = (i % 4) + 1;
  // 1석 요청은 좌석을 차례로 순회, 2~4석 요청은 임의 시작점
  const start = k === 1 ? Math.floor(i / 4) % n : Math.floor(Math.random() * n);
  const seats = [];
  for (let j = 0; j < k; j++) seats.push(SEAT_IDS[(start + j) % n]);
  return seats;
}

// VU 마다 직전 반복 시작 시각(ms). VU 는 각자 JS 런타임을 가지므로 VU 별 값이다
let lastStartMs = 0;

// 반복 i 의 시작 시각까지 기다린다: max(예정 시각, 직전 시작 + VU 간격)
function pace(i) {
  const scheduled = exec.scenario.startTime + (i * 1000) / RATE;
  const target = Math.max(scheduled, lastStartMs + VU_INTERVAL_MS);
  const waitMs = target - Date.now();
  if (waitMs > 0) sleep(waitMs / 1000);
  lastStartMs = Date.now();
}

// ----- 반복 1회 = 사용자 1명의 예매 시도 1건 -----
export default function (data) {
  // 0 ~ TOTAL-1 을 빠짐없이 한 번씩 쓰는 반복 번호(VU 번호는 1부터)
  const vuIndex = (exec.vu.idInTest - 1) % VUS;
  const i = exec.vu.iterationInScenario * VUS + vuIndex;
  const token = data.tokens[i % data.tokens.length];
  const seats = pickSeats(i);

  pace(i);
  reserveRequests.add(1);
  const res = http.post(
    `${BASE_URL}/api/v1/reservation/`,
    JSON.stringify({ screeningId: SCREENING_ID, theaterSeatIdList: seats }),
    {
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      tags: { name: 'POST /api/v1/reservation/' },
      timeout: REQ_TIMEOUT,
    },
  );

  // HTTP 상태 이상(타임아웃 status 0, 401, 500 등)은 별도로 센다
  if (res.status !== 200) reserveHttpError.add(1);

  // 본문의 Result.code 로 판정. 파싱 실패는 null 로 두고 따로 센다
  let code = null;
  try {
    const parsed = res.json('code');
    code = typeof parsed === 'number' ? parsed : null;
  } catch (e) {
    code = null;
  }
  if (code === null) reserveParseError.add(1);

  // 결과 분류: 성공 / 13 / 그 외
  const isSuccess = res.status === 200 && code === CODE_SUCCESS;
  const isTaken = res.status === 200 && code === CODE_SEAT_ALREADY_TAKEN;
  if (isSuccess) {
    reserveSuccess.add(1);
    reserveSeatsWon.add(seats.length);
    durationSuccess.add(res.timings.duration);
  } else if (isTaken) {
    reserveSeatTaken.add(1);
    durationTaken.add(res.timings.duration);
  } else if (code !== null && unexpectedByCode[code]) {
    unexpectedByCode[code].add(1);
  } else if (code !== null) {
    unexpectedUnknownCode.add(1);
  }
  // 0 도 기록해 임계값이 '표본 없음'이 아닌 실제 합계로 평가되게 한다
  reserveUnexpected.add(isSuccess || isTaken ? 0 : 1);
  reserveSeatsWon.add(0);

  check(res, {
    '응답 code 가 성공(0) 또는 SEAT_ALREADY_TAKEN(13)': () => isSuccess || isTaken,
    // 성공이면 data 가 예매 PK(UUID 문자열)여야 한다
    '성공 응답 data 가 UUID': () => !isSuccess || UUID_RE.test(String(res.json('data'))),
  });
}
