// =============================================================================
// 좌석 선점 경쟁 부하 테스트 (POST /api/v1/reservation/)
//
// 목적: 회차 3개(각 1000석)에 회원 150,000명이 1명당 1회씩, 초당 5000건으로 30초간 선점을 시도해
//       uk_seat_hold 가 중복 점유를 막는지(회차마다 정확히 1000석 RESERVED), 나머지 요청이
//       SEAT_ALREADY_TAKEN(13) 또는 혼잡 응답 SERVICE_UNAVAILABLE(28, HTTP 503)으로 끝나는지 본다.
//       (좌석 판정은 부하 후 verify.sh 가 좌석 배치 API 로 한다)
// 측정: 응답 code 별 건수, 보낸 요청 수, 성공 요청이 차지한 좌석 수, 결과별 응답시간.
//
// 부하 모델: constant-arrival-rate — RATE 건/초 x DURATION = 정확히 TOTAL 건(기본 5000 x 30s)
//   - VU 는 PRE_VUS(2000)개 미리 만들고 모자라면 MAX_VUS(5000)까지 늘린다.
//   - VU 가 모자라 시작 못 한 반복은 dropped_iterations 로 남는다(verify.sh 가 FAIL 처리).
//
// 요청 설계
//   - 반복 번호 i = exec.scenario.iterationInTest(0부터, 시나리오 내 유일) → i 번째 회원 토큰.
//     반복마다 다른 회원이므로 회원 1명당 정확히 1회 시도(재시도 없음).
//   - 회차는 i % 회차 수 로 균등 배분, 회차 내 순번 j = floor(i / 회차 수).
//   - 좌석 수 k = (j % 4) + 1 → 1~4석이 1/4 씩. k=1 은 좌석 목록을 차례로 돌아
//     처음 4 x 좌석수 회 안에 모든 좌석이 1석 요청을 받는다(조합이 안 맞아 빈 좌석이 남는 것 방지).
//   - k=2~4 는 임의 시작점에서 연속 k석(목록 끝에서 처음으로 이어짐). 한 요청 안 좌석은 겹치지 않음.
//
// 입력 파일(run.sh 가 만든다, 절대 경로)
//   TOKEN_FILE : ["<access 토큰>", ...] — 인덱스 i = loadtest{i+1} 회원
//   TARGET_FILE: {"screeningIds":[..], "seatIds":[[회차1 좌석..], [회차2 좌석..], ...]}
//
// 실행 예 (보통은 run.sh 가 파일을 준비해 실행한다)
//   k6 run -e TOKEN_FILE=/abs/tokens.json -e TARGET_FILE=/abs/targets.json \
//     --summary-export build/load-test/summary.json scripts/load/seat-race/seat-race.js
// =============================================================================
import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Trend } from 'k6/metrics';

// ----- 설정 (환경변수 + 기본값) -----
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN_FILE = __ENV.TOKEN_FILE || '';
const TARGET_FILE = __ENV.TARGET_FILE || '';
const TOTAL = Number(__ENV.TOTAL || 150000); // 전체 요청 수 = 참여 회원 수
const RATE = Number(__ENV.RATE || 5000); // 초당 시작 요청 수
const DURATION = __ENV.DURATION || '30s'; // 부하 지속 시간(초 단위, RATE x 초 = TOTAL)
const PRE_VUS = Number(__ENV.PRE_VUS || 2000); // 미리 만들어 둘 VU 수
const MAX_VUS = Number(__ENV.MAX_VUS || 5000); // 최대 VU 수(동시 진행 요청 상한)
const REQ_TIMEOUT = __ENV.REQ_TIMEOUT || '30s'; // 요청 1건 타임아웃
// DURATION 이 끝난 뒤 진행 중 요청이 끝나길 기다리는 시간. 넘으면 중단되어 응답 수가 모자란다
const GRACEFUL_STOP = __ENV.GRACEFUL_STOP || '60s';

// ----- 응답 code: 성공은 Result.ofSuccess 의 HttpStatus.OK(200), 실패는 ResponseCode 값 -----
const CODE_SUCCESS = 200;
const CODE_SEAT_ALREADY_TAKEN = 13;
const CODE_SERVICE_UNAVAILABLE = 28; // 풀 대기 초과 등 혼잡, HTTP 503
// 성공 응답 data(예매 PK, UUID v7) 형식
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
// 예상 밖 code 를 이름별로 세기 위한 표 (ResponseCode.java 와 동일, 허용 code 13·28 제외)
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
if (!TOKEN_FILE.startsWith('/') || !TARGET_FILE.startsWith('/')) {
  throw new Error('TOKEN_FILE·TARGET_FILE 을 절대 경로로 지정하세요');
}
if (![TOTAL, RATE, PRE_VUS, MAX_VUS].every((v) => Number.isInteger(v) && v > 0)) {
  throw new Error('TOTAL·RATE·PRE_VUS·MAX_VUS 는 양의 정수여야 합니다');
}
if (MAX_VUS < PRE_VUS) throw new Error('MAX_VUS 는 PRE_VUS 이상이어야 합니다');
// 정확히 TOTAL 건을 시작하려면 RATE x 지속 초 = TOTAL 이어야 한다
const durationMatch = /^([1-9][0-9]*)s$/.exec(DURATION);
if (!durationMatch || RATE * Number(durationMatch[1]) !== TOTAL) {
  throw new Error(`RATE(${RATE}) x DURATION(${DURATION}, 초 단위) 이 TOTAL(${TOTAL}) 과 같아야 합니다`);
}

// ----- 입력 파일: SharedArray 로 한 번만 읽어 모든 VU 가 공유(15만 토큰을 VU 마다 복사하지 않음) -----
const TOKENS = new SharedArray('tokens', () => {
  const list = JSON.parse(open(TOKEN_FILE));
  if (!Array.isArray(list) || list.some((t) => typeof t !== 'string' || t === '')) {
    throw new Error('TOKEN_FILE 은 토큰 문자열 배열이어야 합니다');
  }
  return list;
});
// 회차 id 목록
const SCREENING_IDS = new SharedArray('screeningIds', () => {
  const ids = JSON.parse(open(TARGET_FILE)).screeningIds;
  if (!Array.isArray(ids) || ids.length === 0 || ids.some((id) => !Number.isInteger(id) || id <= 0)) {
    throw new Error('TARGET_FILE.screeningIds 는 양의 정수 배열이어야 합니다');
  }
  return ids;
});
// 좌석 id 를 회차 순서대로 이어 붙인 1차원 배열(회차 s 의 j 번째 좌석 = s x 좌석수 + j)
const SEAT_TABLE = new SharedArray('seatTable', () => {
  const rows = JSON.parse(open(TARGET_FILE)).seatIds;
  if (!Array.isArray(rows) || rows.length === 0) throw new Error('TARGET_FILE.seatIds 가 비어 있습니다');
  const n = rows[0].length;
  rows.forEach((seats, s) => {
    // 회차마다 좌석 수가 같고(4석 이상), 회차 안에서 좌석이 겹치지 않아야 한다
    if (!Array.isArray(seats) || seats.length !== n || n < 4) {
      throw new Error(`TARGET_FILE.seatIds[${s}] 좌석 수가 4 미만이거나 회차마다 다릅니다`);
    }
    if (seats.some((id) => !Number.isInteger(id) || id <= 0) || new Set(seats).size !== n) {
      throw new Error(`TARGET_FILE.seatIds[${s}] 에 잘못되거나 중복된 좌석 id 가 있습니다`);
    }
  });
  return rows.flat();
});
const SCREENING_COUNT = SCREENING_IDS.length;
const SEATS_PER_SCREENING = SEAT_TABLE.length / SCREENING_COUNT;
if (!Number.isInteger(SEATS_PER_SCREENING)) throw new Error('screeningIds 와 seatIds 개수가 맞지 않습니다');
// 회원 1명당 1회: 토큰이 요청 수보다 적으면 같은 회원이 두 번 시도하게 되므로 중단
if (TOKENS.length < TOTAL) throw new Error(`토큰 ${TOKENS.length}개 < TOTAL ${TOTAL}`);

// ----- 사용자 지표 -----
const reserveRequests = new Counter('reserve_requests'); // 실제로 보낸 예매 요청 수
const reserveSuccess = new Counter('reserve_success'); // code 200 (선점 성공) 응답 수
const reserveSeatTaken = new Counter('reserve_seat_taken'); // code 13 응답 수
const reserveBusy = new Counter('reserve_busy'); // code 28 (HTTP 503 혼잡) 응답 수 — 허용
// 성공·13·28 외 모든 결과(다른 code, HTTP 오류·타임아웃, 파싱 실패). 0 이어야 PASS
const reserveUnexpected = new Counter('reserve_unexpected');
const reserveSeatsWon = new Counter('reserve_seats_won'); // 성공 요청이 차지한 좌석 수 합
const reserveHttpError = new Counter('reserve_http_error'); // 허용 외 HTTP 상태(0=타임아웃 등)
const reserveParseError = new Counter('reserve_parse_error'); // 본문 JSON/code 파싱 실패
const reserveNoToken = new Counter('reserve_no_token'); // 반복 번호에 맞는 토큰 없음(발생하면 안 됨)
const durationAll = new Trend('reserve_duration', true); // 예매 요청 전체 응답시간
const durationSuccess = new Trend('reserve_duration_success', true); // 성공 응답시간
const durationTaken = new Trend('reserve_duration_seat_taken', true); // 13 응답시간
const durationBusy = new Trend('reserve_duration_busy', true); // 28 응답시간
// 예상 밖 code 별 카운터(값이 생긴 것만 요약에 표시됨)
const unexpectedByCode = {};
for (const [code, name] of Object.entries(CODE_NAMES)) {
  unexpectedByCode[code] = new Counter(`reserve_code_${code}_${name.toLowerCase()}`);
}
const unexpectedUnknownCode = new Counter('reserve_code_unknown'); // 표에 없는 code

// ----- 부하 모델: 초당 RATE 건 x DURATION = TOTAL 건 -----
export const options = {
  scenarios: {
    reserve: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: PRE_VUS,
      maxVUs: MAX_VUS,
      gracefulStop: GRACEFUL_STOP,
    },
  },
  // 합격 기준 일부를 임계값으로 표현(실패 시 k6 종료 코드 99). 좌석·dropped 판정은 verify.sh
  thresholds: {
    reserve_unexpected: ['count<1'], // 성공·13·28 외 결과 0건
    reserve_requests: [`count==${TOTAL}`], // 보낸 요청 수 = TOTAL
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// 회차 s 의 j 번째 요청이 고를 좌석 id 목록(설계는 파일 상단 주석 참고)
function pickSeats(s, j) {
  const n = SEATS_PER_SCREENING;
  const k = (j % 4) + 1;
  // 1석 요청은 좌석을 차례로 순회, 2~4석 요청은 임의 시작점
  const start = k === 1 ? Math.floor(j / 4) % n : Math.floor(Math.random() * n);
  const seats = [];
  for (let t = 0; t < k; t++) seats.push(SEAT_TABLE[s * n + ((start + t) % n)]);
  return seats;
}

// ----- 반복 1회 = 회원 1명의 예매 시도 1건 -----
export default function () {
  const i = exec.scenario.iterationInTest;
  // 토큰 범위를 벗어나면 같은 회원 재사용이 되므로 보내지 않고 예상 밖으로 센다
  if (i >= TOTAL) {
    reserveNoToken.add(1);
    reserveUnexpected.add(1);
    return;
  }
  const token = TOKENS[i];
  const s = i % SCREENING_COUNT;
  const seats = pickSeats(s, Math.floor(i / SCREENING_COUNT));

  reserveRequests.add(1);
  const res = http.post(
    `${BASE_URL}/api/v1/reservation/`,
    JSON.stringify({ screeningId: SCREENING_IDS[s], theaterSeatIdList: seats }),
    {
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      tags: { name: 'POST /api/v1/reservation/' },
      timeout: REQ_TIMEOUT,
    },
  );
  durationAll.add(res.timings.duration);

  // 본문의 Result.code 로 판정. 파싱 실패는 null 로 두고 따로 센다
  let code = null;
  try {
    const parsed = res.json('code');
    code = typeof parsed === 'number' ? parsed : null;
  } catch (e) {
    code = null;
  }
  if (code === null) reserveParseError.add(1);

  // 결과 분류: 성공 / 13 / 28(HTTP 503) / 그 외
  const isSuccess = res.status === 200 && code === CODE_SUCCESS;
  const isTaken = res.status === 200 && code === CODE_SEAT_ALREADY_TAKEN;
  const isBusy = res.status === 503 && code === CODE_SERVICE_UNAVAILABLE;
  // 허용 조합(200·503) 밖의 HTTP 상태(타임아웃 status 0, 401, 500 등)는 별도로 센다
  if (!isSuccess && !isTaken && !isBusy && res.status !== 200) reserveHttpError.add(1);
  if (isSuccess) {
    reserveSuccess.add(1);
    reserveSeatsWon.add(seats.length);
    durationSuccess.add(res.timings.duration);
  } else if (isTaken) {
    reserveSeatTaken.add(1);
    durationTaken.add(res.timings.duration);
  } else if (isBusy) {
    reserveBusy.add(1);
    durationBusy.add(res.timings.duration);
  } else if (code !== null && unexpectedByCode[code]) {
    unexpectedByCode[code].add(1);
  } else if (code !== null) {
    unexpectedUnknownCode.add(1);
  }
  const allowed = isSuccess || isTaken || isBusy;
  // 0 도 기록해 임계값이 '표본 없음'이 아닌 실제 합계로 평가되게 한다
  reserveUnexpected.add(allowed ? 0 : 1);
  reserveSeatsWon.add(0);

  check(res, {
    '응답 code 가 성공(200)·SEAT_ALREADY_TAKEN(13)·SERVICE_UNAVAILABLE(28)': () => allowed,
    // 성공이면 data 가 예매 PK(UUID 문자열)여야 한다
    '성공 응답 data 가 UUID': () => !isSuccess || UUID_RE.test(String(res.json('data'))),
  });
}
