# Step 2 — 경계 계층

> 구현 step 하나. state.md 로드맵의 2행에 대응합니다. 계획 블록(1~4)은 착수 전, 기록 블록(5~8)은 진행 중 순서대로 채웁니다.
> 막힘·변경은 §8 에 적고, 새 step 이 필요하면 state.md 변경 이력에 올립니다.

---

## 0. Metadata

| Field | Value |
|---|---|
| Step | 2 |
| Title | 경계 계층 |
| Feature | trading-web-shadcn-rebuild |
| Based on | development-plan §5-2, D4·D5·D6·D7·D9·D14, §6 4·4-1·6행, §8-3 AS-01~03, §9 경계 |
| Status | done |
| Owner | Maker |

---

**━━ Plan block (착수 전) ━━**

## 1. Goal and scope  (plan)

- Builds: 서버 응답을 금융 값 손실 없이 받는 API 공통 틀과, 화면이 금융 값을 문자열로만 다루게 하는 유틸을 시험 먼저(golden)로 만든다.
- Scope (where this step stops): `src/shared/api/`(무손실 본문 파싱, 디코더 공통 도우미, 서비스별 오류 정규화, 호출 단일 경로)와 `src/shared/lib/`(decimal·rate·quantity·chart-coordinate·mask·idempotency). 엔드포인트별 디코더·화면용 호출·TanStack Query 는 다음 step 몫. 금액 산술 함수는 두지 않는다.

---

## 2. Approach (how, in what order)  (plan)

- 계획 §6 4행 golden(`1000000`, `1000000.0000`, `0.1`, `-1234.5`, `1E-8`, `12345678901234567890.12`, null «-», 마스킹, 비율 2자리 half-up)을 시험으로 먼저 쓰고 Red 를 확인한다.
- 아래 층부터 구현: lib(decimal → rate·quantity·chart-coordinate·mask·idempotency) → api(parse-json → decode → error → http).
- 소스 검사 시험의 float 금지 허용 목록에 안전 정수 변환과 `chart-coordinate.ts` 만 추가한다.

---

## 3. Dependencies  (plan)

- Previous step: step-1 (교체 골격, 커밋 `8e5bd7c`)
- Prerequisites: `lossless-json` 4.3.3 이 Node 20·jsdom 에서 동작할 것(아니면 멈추고 보고)

---

## 4. Completion criteria  (plan)

- [x] golden 시험 통과(§6 4행 값 전부, 마스킹, 비율 half-up)
- [x] 소스 검사 시험: 허용 목록 2개 파일 밖 `parseFloat`·`Number(`·`parseInt(`·`Math.` 0건
- [x] `npm run build`·`npm run lint`·`npm test -- --run` 모두 0 종료(Node 20.19.0 에서도)

---

**━━ Record block (진행 중 순서대로) ━━**

## 5. Work log  (record)

1. `lossless-json@4.3.3` 정확 버전 설치(`--save-exact`). `parse(text, undefined, { parseNumber: (s) => s })` 로 숫자 토큰을 원문 문자열로 받음. 같은 키 중복은 `SyntaxError` 로 거부(실측), 빈 문자열도 `SyntaxError`.
2. 시험 먼저: 시험 파일 11개(lib 7 · api 4)를 쓰고 구현 없이 돌려 11개 파일이 import 실패(Red)임을 확인.
3. lib 구현: `safe-integer`(BigInt 범위 검사 뒤 변환) → `decimal`(정규화·분해·비교·부호·tone·소수점 이동·half-up 반올림·표시·원화 AS-02) → `rate`(`RateFraction`·`RatePercent` 브랜드 타입, 단위 변환, 소수 2자리 표시) → `quantity`(BigInt 2함수) → `chart-coordinate` → `mask` → `idempotency`.
4. api 구현: `parse-json` → `decode`(unknown + 타입 가드, `DecodeError`) → `error`(`ApiError`, 서비스별 모양 정규화) → `http`(`requestJson` 단일 호출 경로).
5. `src/test/guards/source-guards.test.ts` 허용 목록에 `src/shared/lib/safe-integer.ts`·`src/shared/lib/chart-coordinate.ts` 추가, 허용 파일 존재 확인 시험 1건 추가.
6. 시험 도우미 결함 1건(§8)과 `tsc` 가 잡은 시험 호출 인자 오류 1건을 고침.

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
| Red 확인 | 구현 전 `npx vitest run src/shared` | 11개 파일 import 실패 |
| golden·단위 시험 | `npx vitest run src/shared` | 13개 파일 · 220 통과 (신규 11개 파일) |
| 전체 시험 | `npm test -- --run` (Node 26) | 20개 파일 · 252 통과 / 0 실패 (step-1 은 9개 파일 36건) |
| build | `npm run build` (`tsc -b && vite build`) | 0 종료 |
| lint | `npm run lint` | 0 종료 |
| Node 20.19.0 | 같은 vitest·tsc·eslint 를 Node 20.19.0 으로 실행 | 20개 파일 통과, tsc·eslint 0 종료 |
| float 금지 소스 검사 | 허용 목록 2개 파일 밖 | 0건 |
| 타입 수준 시험 | `RateFraction` ↔ `RatePercent` 혼용이 `@ts-expect-error` 로 막힘을 `tsc` 가 검사 | 통과 |

---

## 7. Result  (record)

- Done: API 공통 틀(무손실 파싱·디코더 도우미·오류 정규화·`requestJson`)과 금융 값 유틸 6종(decimal·rate·quantity·chart-coordinate·mask·idempotency)을 시험 먼저로 완성.
- Changed files / PR: 커밋 해시는 보고에 기록(푸시 안 함).

---

## 8. Problems found  (record)

| Problem | How it was handled |
|---|---|
| 계획은 «안전 정수 디코더»를 float 허용 파일로 적었으나 위치를 정하지 않았다. `api` 에 두면 `decimal`(지수 파싱)·`quantity`(진행률)가 `api` 를 import 하는 역방향이 생김 | 변환을 `src/shared/lib/safe-integer.ts` 한 곳에 둠(`api/decode.ts` 가 이를 사용). 허용 목록 = `safe-integer.ts` + `chart-coordinate.ts` 로 계획 §6 4행(두 곳)과 같은 개수 |
| 시험 도우미 `stubFetch` 가 `instanceof Error` 로 던질 값을 가렸는데 jsdom 의 `DOMException` 은 Node 의 `Error` 를 상속하지 않아 AbortError 시험이 실패 | 구현이 아니라 시험 도구의 결함. 응답은 `text` 를 가진 객체, 그 밖은 던질 값으로 가르도록 도우미를 고침 |
| `tsc` 가 시험의 `new DecodeError(path, expected)`(인자 2개)를 잡음. `DecodeError` 는 값 자체는 싣지 않고 받은 값의 «형»만 문구에 쓰려고 인자 3개를 받음 | 시험 호출에 세 번째 인자를 넘기도록 고침(기대값은 그대로) |
| 커밋 게이트(API 오류 규칙 AE21)가 `http.ts` 의 `catch {}`(JSON 아닌 오류 본문을 원문 문자열로 대체)를 «삼킨 예외»로 차단 | 우회 변수 없이 구조를 고침: `SyntaxError` 로 좁혀 원문을 `rawMessage` 로 노출하고 그 밖의 예외는 다시 던짐(`errorFromResponse`). 동작·시험 결과 동일 |
| 간헐 실패(step-1 시험, 이번 범위 밖): `npm test -- --run` 1회가 «시험 252건 통과, 처리 안 된 오류 2건(`window.matchMedia is not a function`, `src/main.test.tsx`)» 으로 종료 코드 1. 이후 전체 6회·단독 6회는 모두 정상(1/약 16회). 원인 추정: `main.test.tsx` 가 `h1` 이 보이는 즉시 끝나 `afterEach` 가 `matchMedia` 대역을 푸는데 `ThemeProvider` 의 passive effect(`getSystemTheme`)가 그 뒤에 돌 수 있음 | 이번 step 에서 고치지 않음(step-1 파일). 제안: 시험이 `document.documentElement` 의 `light`/`dark` 클래스까지 기다린 뒤 끝나도록 보강. CI 에서 재발하면 step-3 앞에서 처리 |
| 계획에 없는 결정 3건 | 아래 «가정» 참고 |

가정(사소·되돌리기 쉬움, 필요하면 바꿀 수 있음):

| 항목 | 가정 | 근거 |
|---|---|---|
| 지수 상한 | `normalizeDecimal` 은 지수 절댓값이 1000 을 넘으면 null(디코드 오류) | `1E9999` 입력이 수천 자 문자열을 만들어 화면을 멈추는 것을 막음. BigDecimal 실사용 자릿수(`1E-8`)보다 충분히 큼 |
| 형식 이상 계좌번호 | 길이를 드러내지 않도록 고정 `********` 로 가림 | AS-01 의 «전체를 가림» 해석 |
| 정규화 시 소수부 끝 0 | 보존(`1000000.0000` 은 그대로, 표시에서만 AS-02 로 정수화) | 계획 §6 «원문 보존·평문화» |
| 남은 수량 음수 | 체결 > 주문이면 클램프하지 않고 음수로 반환 | 서버 불일치를 가리지 않음(silent default 금지) |
| 사용자 오류 문구 | status 만으로 고름(400·422, 401·403, 404, 409, 429, 5xx, 그 밖 4xx, 연결 실패, 응답 형식 오류) | 백엔드 오류 코드는 도메인별로 소수라 화면 step 에서 필요할 때 code 별 문구를 얹음 |

후속 제안(이번 범위 밖): `fetch(`·`.json(` 호출이 `src/shared/api/http.ts` 밖에 있으면 실패하는 소스 검사 시험(D7 «호출 경계 한 곳»). 화면 step 에서 호출이 생기기 시작할 때 추가하면 좋음.
